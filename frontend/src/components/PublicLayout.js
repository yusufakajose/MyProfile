import React from 'react';
import Box from '@mui/material/Box';

const PublicLayout = ({ children }) => {
  return (
    <>
      <a href="#main" className="skip-link">Skip to content</a>
      <Box component="main" id="main" sx={{ minHeight: '100vh' }}>
        {children}
      </Box>
    </>
  );
};

export default PublicLayout;


