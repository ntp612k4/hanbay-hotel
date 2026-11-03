import { theme, type ThemeConfig } from 'antd';

export const aegeanTheme: ThemeConfig = {
  algorithm: theme.defaultAlgorithm,
  token: {
    colorPrimary: '#0f172a',
    colorInfo: '#0284c7',
    colorLink: '#0284c7',
    colorSuccess: '#059669',
    colorWarning: '#d97706',
    colorError: '#ba1a1a',
    colorBgBase: '#f7f9fb',
    colorBgContainer: '#ffffff',
    colorBgElevated: '#ffffff',
    colorText: '#191c1e',
    colorTextSecondary: '#45464d',
    colorBorder: '#e2e8f0',
    colorBorderSecondary: '#f1f5f9',
    fontFamily:
      "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
    fontSize: 16,
    borderRadius: 4,
    borderRadiusLG: 8,
    controlHeight: 40,
    lineWidth: 1,
    boxShadow: 'none',
    boxShadowSecondary: '0 20px 25px -5px rgba(15, 23, 42, 0.1)',
  },
  components: {
    Button: {
      borderRadius: 4,
      primaryShadow: 'none',
      defaultShadow: 'none',
      dangerShadow: 'none',
      fontWeight: 600,
    },
    Card: {
      borderRadiusLG: 8,
      boxShadow: 'none',
      headerFontSize: 18,
    },
    Input: {
      activeBorderColor: '#0284c7',
      activeShadow: '0 0 0 2px rgba(2, 132, 199, 0.16)',
      hoverBorderColor: '#0f172a',
    },
    Select: {
      activeBorderColor: '#0284c7',
      activeOutlineColor: 'rgba(2, 132, 199, 0.16)',
      hoverBorderColor: '#0f172a',
      optionActiveBg: '#f0f9ff',
      optionSelectedBg: '#e0f2fe',
      optionSelectedColor: '#0c4a6e',
      optionSelectedFontWeight: 600,
    },
    Table: {
      borderColor: '#f1f5f9',
      headerBg: '#f2f4f6',
      headerColor: '#45464d',
      rowHoverBg: '#f0f9ff',
    },
    Tag: {
      borderRadiusSM: 9999,
    },
    Modal: {
      borderRadiusLG: 8,
      boxShadow: '0 20px 25px -5px rgba(15, 23, 42, 0.1)',
    },
  },
};
