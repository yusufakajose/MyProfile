# LinkGrove Analytics Dashboard Frontend

A modern React-based analytics dashboard for LinkGrove, featuring real-time charts and performance metrics.

## 🚀 Features

- **📊 Interactive Charts** - Line charts, bar charts, and pie charts using Recharts
- **📈 Time Series Analytics** - Track click trends over customizable time periods
- **🎯 Performance Metrics** - Total clicks, engagement rates, and top-performing links
- **🎨 Modern UI** - Material-UI components with responsive design
- **⚡ Real-time Data** - Live updates from the LinkGrove API

## 🛠️ Tech Stack

- **React 18** - Modern React with hooks
- **Recharts** - Beautiful, composable charting library
- **Material-UI** - React UI framework
- **Axios** - HTTP client for API calls
- **React Router** - Client-side routing

## 📦 Installation

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Start the development server:**
   ```bash
   npm start
   ```

3. **Open your browser:**
   Navigate to [http://localhost:3000](http://localhost:3000)

## 🔧 Configuration

The frontend is configured to proxy API requests to the backend at `http://localhost:8080`. Make sure the LinkGrove backend is running before using the dashboard.

## 📊 Dashboard Features

### Summary Cards
- **Total Clicks** - Overall click performance
- **Total Links** - Number of active links
- **Active Links** - Currently active links
- **Average Clicks per Link** - Engagement metrics

### Interactive Charts
- **Click Trends** - Line chart showing daily click patterns
- **Top Performing Links** - Bar chart of best-performing links
- **Link Status Distribution** - Pie chart of active vs inactive links
- **Most Popular Link** - Highlight of the best-performing link

### Time Range Selection
- **7 days** - Last week's data
- **14 days** - Last two weeks
- **30 days** - Last month

## 🎨 Customization

### Styling
The dashboard uses Material-UI theming. Customize colors and styling in `src/index.js`:

```javascript
const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2', // Your brand color
    },
    // ... other theme options
  },
});
```

### Charts
Modify chart configurations in `src/components/AnalyticsDashboard.js`. Recharts provides extensive customization options for colors, animations, and interactions.

## 🔌 API Integration

The dashboard connects to these LinkGrove API endpoints:

- `GET /api/analytics/dashboard/summary` - Dashboard summary metrics
- `GET /api/analytics/dashboard/timeseries?days={n}` - Time series data
- `GET /api/analytics/top-links` - Top performing links

## 🚀 Production Build

```bash
npm run build
```

This creates an optimized production build in the `build/` folder.

## 📱 Responsive Design

The dashboard is fully responsive and works on:
- Desktop computers
- Tablets
- Mobile phones

## 🔒 Authentication

Currently uses a mock JWT token for demonstration. In production, integrate with your authentication system by:

1. Replacing the mock token in `AnalyticsDashboard.js`
2. Implementing proper token management
3. Adding login/logout functionality

## 🐛 Troubleshooting

### Common Issues

1. **Charts not loading** - Check that the backend API is running
2. **CORS errors** - Ensure the backend has CORS configured
3. **Data not updating** - Verify the API endpoints are working

### Development Tips

- Use browser dev tools to inspect API responses
- Check the console for error messages
- Verify network connectivity to the backend

## 📈 Performance

- **Lazy loading** - Charts load data on demand
- **Caching** - API responses are cached by the backend
- **Optimized rendering** - React.memo and useMemo for performance

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

This project is part of LinkGrove and follows the same license terms.
