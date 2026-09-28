import { createTheme } from '@mui/material/styles';

export const getTheme = (mode: 'light' | 'dark') => {
  return createTheme({
    palette: {
      mode,
      primary: {
        main: mode === 'dark' ? '#818cf8' : '#4f46e5', // Sleek indigo
        light: '#a5b4fc',
        dark: '#3730a3',
      },
      secondary: {
        main: mode === 'dark' ? '#34d399' : '#059669', // Emerald green
      },
      background: {
        default: mode === 'dark' ? '#0f172a' : '#f8fafc', // Slate background
        paper: mode === 'dark' ? '#1e293b' : '#ffffff',
      },
      text: {
        primary: mode === 'dark' ? '#f8fafc' : '#0f172a',
        secondary: mode === 'dark' ? '#94a3b8' : '#475569',
      },
      divider: mode === 'dark' ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.08)',
    },
    typography: {
      fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
      h1: {
        fontWeight: 700,
        fontSize: '2.5rem',
      },
      h4: {
        fontWeight: 600,
        fontSize: '1.75rem',
        letterSpacing: '-0.02em',
      },
      h5: {
        fontWeight: 600,
        fontSize: '1.25rem',
        letterSpacing: '-0.01em',
      },
      h6: {
        fontWeight: 600,
        fontSize: '1rem',
      },
      subtitle1: {
        fontSize: '0.875rem',
        fontWeight: 500,
      },
      body1: {
        fontSize: '0.875rem',
        lineHeight: 1.5,
      },
      button: {
        textTransform: 'none',
        fontWeight: 600,
      },
    },
    shape: {
      borderRadius: 12,
    },
    components: {
      MuiButton: {
        styleOverrides: {
          root: {
            borderRadius: '8px',
            padding: '8px 16px',
            transition: 'all 0.2s ease-in-out',
            '&:hover': {
              transform: 'translateY(-1px)',
            },
            '&.MuiButton-containedPrimary': {
              background: mode === 'dark' 
                ? 'linear-gradient(135deg, #6366f1 0%, #4f46e5 100%)'
                : 'linear-gradient(135deg, #4f46e5 0%, #3730a3 100%)',
            },
          },
          contained: {
            boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            backgroundImage: 'none',
            borderRadius: '16px',
            boxShadow: mode === 'dark'
              ? '0 4px 20px 0 rgba(0,0,0,0.3)'
              : '0 4px 20px 0 rgba(0,0,0,0.05)',
            border: mode === 'dark' ? '1px solid rgba(255, 255, 255, 0.05)' : '1px solid rgba(0, 0, 0, 0.03)',
            transition: 'transform 0.3s ease-in-out, box-shadow 0.3s ease-in-out',
            '&:hover': {
              transform: 'translateY(-2px)',
              boxShadow: mode === 'dark'
                ? '0 10px 25px 0 rgba(0,0,0,0.4)'
                : '0 10px 25px 0 rgba(0,0,0,0.08)',
            },
          },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: {
            backgroundImage: 'none',
          },
        },
      },
      MuiTableCell: {
        styleOverrides: {
          root: {
            borderBottom: mode === 'dark' ? '1px solid rgba(255, 255, 255, 0.08)' : '1px solid rgba(0, 0, 0, 0.08)',
          },
          head: {
            fontWeight: 600,
            backgroundColor: mode === 'dark' ? '#1e293b' : '#f1f5f9',
          },
        },
      },
    },
  });
};
