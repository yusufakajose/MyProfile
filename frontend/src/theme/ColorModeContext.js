import React from 'react';

export const ColorModeContext = React.createContext({
  mode: 'light',
  toggleColorMode: () => {},
});

export default ColorModeContext;


