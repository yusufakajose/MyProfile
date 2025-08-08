import React, { useState, useEffect } from 'react';
import {
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  CircularProgress,
  Alert,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Paper,
  Button
} from '@mui/material';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell
} from 'recharts';
import {
  TrendingUp,
  Link as LinkIcon,
  Visibility,
  Star
} from '@mui/icons-material';
import client from '../api/client';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8'];

const AnalyticsDashboard = () => {
  const [summaryData, setSummaryData] = useState(null);
  const [timeseriesData, setTimeseriesData] = useState(null);
  const [topLinksData, setTopLinksData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [timeRange, setTimeRange] = useState(7);

  useEffect(() => {
    fetchAnalyticsData();
  }, [timeRange]);

  const fetchAnalyticsData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const [summaryRes, timeseriesRes, topLinksRes] = await Promise.all([
        client.get('/analytics/dashboard/summary'),
        client.get(`/analytics/dashboard/timeseries?days=${timeRange}`),
        client.get('/analytics/top-links')
      ]);

      setSummaryData(summaryRes.data);
      setTimeseriesData(timeseriesRes.data);
      setTopLinksData(topLinksRes.data);
    } catch (err) {
      console.error('Error fetching analytics data:', err);
      setError('Failed to load analytics data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const buildCsv = (rows, columns) => {
    const escape = (v) => {
      if (v == null) return '';
      const s = String(v).replace(/\"/g, '""');
      return /[",\n]/.test(s) ? `"${s}"` : s;
    };
    const header = columns.map(([h]) => h).join(',');
    const body = rows.map((r) => columns.map(([, getter]) => escape(getter(r))).join(',')).join('\n');
    return `${header}\n${body}\n`;
  };

  const triggerDownload = (csv, filename) => {
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  };

  const downloadCsv = async (path, filename, fallbackRows, columns) => {
    try {
      const res = await client.get(path, { responseType: 'blob' });
      const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8;' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (e) {
      console.warn('Server CSV export failed, using client-side fallback', e);
      if (fallbackRows && columns) {
        const csv = buildCsv(fallbackRows, columns);
        triggerDownload(csv, filename);
      }
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ mb: 2 }}>
        {error}
      </Alert>
    );
  }

  return (
    <Box>
      {/* Time Range Selector */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Analytics Overview
        </Typography>
        <FormControl sx={{ minWidth: 120 }}>
          <InputLabel>Time Range</InputLabel>
          <Select
            value={timeRange}
            label="Time Range"
            onChange={(e) => setTimeRange(e.target.value)}
          >
            <MenuItem value={7}>Last 7 days</MenuItem>
            <MenuItem value={14}>Last 14 days</MenuItem>
            <MenuItem value={30}>Last 30 days</MenuItem>
          </Select>
        </FormControl>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            variant="outlined"
            onClick={() => downloadCsv(
              `/analytics/export/timeseries?days=${timeRange}`,
              `analytics_timeseries_${timeRange}d.csv`,
              (timeseriesData?.timeseriesData || []),
              [
                ['date', r => r.date],
                ['clicks', r => r.clicks],
                ['uniqueVisitors', r => r.uniqueVisitors]
              ]
            )}
          >
            Export Timeseries CSV
          </Button>
          <Button
            variant="outlined"
            onClick={() => downloadCsv(
              `/analytics/export/top-links`,
              `analytics_top_links.csv`,
              (topLinksData?.topLinks || []),
              [
                ['id', r => r.id],
                ['title', r => r.title],
                ['url', r => r.url],
                ['clickCount', r => r.clickCount],
                ['displayOrder', r => r.displayOrder]
              ]
            )}
          >
            Export Top Links CSV
          </Button>
        </Box>
      </Box>

      {/* Summary Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <TrendingUp color="primary" sx={{ mr: 1 }} />
                <Typography color="textSecondary" gutterBottom>
                  Total Clicks
                </Typography>
              </Box>
              <Typography variant="h4" component="div">
                {summaryData?.totalClicks || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <LinkIcon color="primary" sx={{ mr: 1 }} />
                <Typography color="textSecondary" gutterBottom>
                  Total Links
                </Typography>
              </Box>
              <Typography variant="h4" component="div">
                {summaryData?.totalLinks || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <Visibility color="primary" sx={{ mr: 1 }} />
                <Typography color="textSecondary" gutterBottom>
                  Active Links
                </Typography>
              </Box>
              <Typography variant="h4" component="div">
                {summaryData?.activeLinks || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center">
                <Star color="primary" sx={{ mr: 1 }} />
                <Typography color="textSecondary" gutterBottom>
                  Avg Clicks/Link
                </Typography>
              </Box>
              <Typography variant="h4" component="div">
                {summaryData?.averageClicksPerLink || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Charts */}
      <Grid container spacing={3}>
        {/* Time Series Chart */}
        <Grid item xs={12} lg={8}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Click Trends ({timeRange} days)
            </Typography>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={timeseriesData?.timeseriesData || []}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Line 
                  type="monotone" 
                  dataKey="clicks" 
                  stroke="#8884d8" 
                  strokeWidth={2}
                  name="Clicks"
                />
                <Line 
                  type="monotone" 
                  dataKey="uniqueVisitors" 
                  stroke="#82ca9d" 
                  strokeWidth={2}
                  name="Unique Visitors"
                />
              </LineChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>

        {/* Top Links Chart */}
        <Grid item xs={12} lg={4}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Top Performing Links
            </Typography>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={topLinksData?.topLinks || []}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="title" angle={-45} textAnchor="end" height={80} />
                <YAxis />
                <Tooltip />
                <Bar dataKey="clickCount" fill="#8884d8" />
              </BarChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>

        {/* Engagement Metrics */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Link Status Distribution
            </Typography>
            <ResponsiveContainer width="100%" height={250}>
              <PieChart>
                <Pie
                  data={[
                    { name: 'Active', value: summaryData?.activeLinks || 0 },
                    { name: 'Inactive', value: summaryData?.inactiveLinks || 0 }
                  ]}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {COLORS.map((color, index) => (
                    <Cell key={`cell-${index}`} fill={color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>

        {/* Most Popular Link */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Most Popular Link
            </Typography>
            {summaryData?.mostPopularLink ? (
              <Box>
                <Typography variant="h5" color="primary" gutterBottom>
                  {summaryData.mostPopularLink.title}
                </Typography>
                <Typography variant="h3" color="secondary">
                  {summaryData.mostPopularLink.clickCount} clicks
                </Typography>
                <Typography variant="body2" color="textSecondary" sx={{ mt: 1 }}>
                  This is your best performing link
                </Typography>
              </Box>
            ) : (
              <Typography variant="body1" color="textSecondary">
                No links with clicks yet
              </Typography>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default AnalyticsDashboard;
