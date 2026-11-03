import { RouterProvider } from 'react-router-dom';

import { AiAssistantWidget } from '../features/ai-assistant';
import { AppProviders } from './providers';
import { router } from './router';

function App() {
  return (
    <AppProviders>
      <RouterProvider router={router} />
      <AiAssistantWidget navigateTo={(location) => void router.navigate(location)} />
    </AppProviders>
  );
}

export default App;
