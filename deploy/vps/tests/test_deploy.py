"""Run the real shell deployment against fake Docker/HTTP commands in a temp VPS."""
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

DEPLOY = Path(__file__).resolve().parents[1] / 'deploy.sh'
FAKE_DOCKER = r'''
import json, os, pathlib, sys
args = sys.argv[1:]
values = dict(line.split('=', 1) for line in pathlib.Path('.env').read_text().splitlines() if '=' in line)
entry = {'args': args, 'backend': os.environ.get('BACKEND_IMAGE', values['BACKEND_IMAGE']),
         'frontend': os.environ.get('FRONTEND_IMAGE', values['FRONTEND_IMAGE'])}
with open('docker-calls.jsonl', 'a') as f:
    f.write(json.dumps(entry) + '\n')
failure = os.environ.get('FAIL_STAGE')
if 'pg_dump' in ' '.join(args):
    if failure == 'backup': sys.exit(1)
    print('database-backup')
elif args[:2] == ['compose', 'config']:
    if '--quiet' in args and failure == 'config': sys.exit(1)
    if '--format' in args:
        print(json.dumps({'services': {'frontend': {'ports': [{'published': '8099'}]}}}))
elif args[:2] == ['compose', 'pull']:
    if failure == 'pull': sys.exit(1)
elif args[:2] == ['compose', 'up']:
    if entry['backend'] == 'backend:new' and failure == 'start': sys.exit(1)
    if entry['backend'] == 'backend:old' and os.environ.get('FAIL_ROLLBACK'): sys.exit(1)
'''


class DeploymentTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        self.release = self.root / 'releases' / 'candidate'
        self.release.mkdir(parents=True)
        self.bin = self.root / 'bin'
        self.bin.mkdir()
        self.original = {
            '.env': 'BACKEND_IMAGE=backend:old\nFRONTEND_IMAGE=frontend:old\nMINIO_ACCESS_KEY=existing\nMINIO_SECRET_KEY=existing-secret\n',
            'docker-compose.yml': 'old-compose\n',
            'nginx.conf': 'old-nginx\n',
        }
        for file, content in self.original.items():
            (self.root / file).write_text(content)
        for file in ('docker-compose.yml', 'nginx.conf', 'migrate-images.py', 'smoke-test.py',
                     'minio-console.conf', 'install-minio-console.sh'):
            (self.release / file).write_text('new-' + file)
        (self.release / 'verify-public.py').write_text(
            'import os,sys\nsys.exit(1 if os.environ.get("FAIL_STAGE") == "public" else 0)\n')
        self.command('docker', FAKE_DOCKER)
        self.command('flock', 'import os,sys\nsys.exit(1 if os.environ.get("FAIL_STAGE") == "lock" else 0)')
        self.command('curl', 'import pathlib,sys\npathlib.Path("curl-url").write_text(sys.argv[-1])')

    def command(self, name, script):
        path = self.bin / name
        path.write_text('#!' + sys.executable + '\n' + script)
        path.chmod(0o755)

    def run_deploy(self, failure='', rollback_failure=False, release=None):
        env = {**os.environ, 'PATH': str(self.bin) + os.pathsep + os.environ['PATH'],
               'HOTELAPP_DIR': str(self.root), 'BACKEND_IMAGE': 'backend:new',
               'FRONTEND_IMAGE': 'frontend:new', 'FAIL_STAGE': failure}
        env.pop('HOTEL_MAINTENANCE_CREDENTIALS', None)
        if rollback_failure:
            env['FAIL_ROLLBACK'] = '1'
        return subprocess.run(['bash', str(DEPLOY), str(release or self.release)],
                              env=env, capture_output=True, text=True, timeout=15)

    def calls(self):
        path = self.root / 'docker-calls.jsonl'
        return [json.loads(line) for line in path.read_text().splitlines()] if path.exists() else []

    def assert_restored(self):
        for file, content in self.original.items():
            self.assertEqual((self.root / file).read_text(), content)

    def test_success_persists_new_tags_and_keeps_backup(self):
        result = self.run_deploy()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('BACKEND_IMAGE=backend:new', (self.root / '.env').read_text())
        self.assertIn('MINIO_SECRET_KEY=existing-secret', (self.root / '.env').read_text())
        self.assertEqual((self.root / '.env').stat().st_mode & 0o777, 0o600)
        backup = next((self.root / 'backups').iterdir())
        self.assertEqual((backup / '.env').read_text(), self.original['.env'])
        self.assertEqual((backup / 'database.dump').read_text(), 'database-backup\n')

    def test_failures_before_container_update_restore_without_restart(self):
        for failure in ('config', 'pull'):
            with self.subTest(failure=failure):
                result = self.run_deploy(failure)
                self.assertNotEqual(result.returncode, 0)
                self.assert_restored()
                self.assertFalse(any(c['args'][:2] == ['compose', 'up'] for c in self.calls()))

    def test_unhealthy_release_restores_old_images_despite_exported_new_tags(self):
        result = self.run_deploy('start')
        self.assertNotEqual(result.returncode, 0)
        self.assert_restored()
        updates = [c for c in self.calls() if c['args'][:2] == ['compose', 'up']]
        self.assertEqual([c['backend'] for c in updates], ['backend:new', 'backend:old'])
        self.assertEqual(updates[-1]['frontend'], 'frontend:old')
        self.assertIn('127.0.0.1:8099/', (self.root / 'curl-url').read_text())
        self.assertIn('Previous application restored', result.stderr)

    def test_public_verification_failure_rolls_back_and_fails_job(self):
        result = self.run_deploy('public')
        self.assertNotEqual(result.returncode, 0)
        self.assert_restored()
        self.assertIn('Previous application restored', result.stderr)

    def test_failed_rollback_is_reported_without_database_restore(self):
        result = self.run_deploy('start', rollback_failure=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('ROLLBACK FAILED', result.stderr)
        self.assertFalse(any('pg_restore' in ' '.join(c['args']) for c in self.calls()))

    def test_backup_failure_leaves_application_untouched(self):
        result = self.run_deploy('backup')
        self.assertNotEqual(result.returncode, 0)
        self.assert_restored()
        self.assertFalse(any(c['args'][:2] == ['compose', 'up'] for c in self.calls()))

    def test_concurrent_deployment_exits_before_backup_or_mutation(self):
        result = self.run_deploy('lock')
        self.assertNotEqual(result.returncode, 0)
        self.assert_restored()
        self.assertEqual(self.calls(), [])

    def test_release_symlink_outside_release_tree_is_rejected(self):
        outside = self.root / 'outside'
        outside.mkdir()
        link = self.root / 'releases' / 'link'
        link.symlink_to(outside, target_is_directory=True)
        result = self.run_deploy(release=link)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Invalid release directory', result.stdout)
        self.assertEqual(self.calls(), [])


if __name__ == '__main__':
    unittest.main()
