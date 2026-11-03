// @vitest-environment jsdom

import '@testing-library/jest-dom/vitest';

import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';

import { TrendChart } from './trend-chart';

describe('TrendChart', () => {
  afterEach(cleanup);

  it('keeps negative and positive values inside the plot and renders a zero baseline', () => {
    const { container } = render(
      <TrendChart
        title="Doanh thu"
        description="Doanh thu thuần"
        points={[
          { date: '2026-08-01', value: 1_200_000 },
          { date: '2026-08-02', value: -200_000 },
        ]}
        color="#059669"
        formatValue={String}
      />,
    );

    expect(container.querySelector('.manager-dashboard__zero-line')).toBeInTheDocument();
    const yCoordinates = [...container.querySelectorAll('circle')].map((node) =>
      Number(node.getAttribute('cy')),
    );
    expect(yCoordinates).toHaveLength(2);
    expect(yCoordinates.every((value) => value >= 24 && value <= 216)).toBe(true);
    expect(screen.getByLabelText('Doanh thu')).toBeInTheDocument();
  });
});
