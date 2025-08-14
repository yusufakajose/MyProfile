import React, { useState, useEffect, useCallback } from 'react';
import {
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  Stack,
  Alert,
  Skeleton,
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
  const [referrersData, setReferrersData] = useState(null);
  const [devicesData, setDevicesData] = useState(null);
  const [sourcesData, setSourcesData] = useState(null);
  const [countriesData, setCountriesData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [timeRange, setTimeRange] = useState(7);
  const [selectedLinkId, setSelectedLinkId] = useState('');
  const [perLinkSeries, setPerLinkSeries] = useState(null);
  const [variantsData, setVariantsData] = useState(null);
  const [perLinkVariantsData, setPerLinkVariantsData] = useState(null);
  const [perLinkSourcesData, setPerLinkSourcesData] = useState(null);
  const [sourceFilter, setSourceFilter] = useState('');

  const renderLoading = () => (
    <Box>
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Skeleton variant="text" width={240} height={40} />
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Skeleton variant="rounded" width={120} height={40} />
          <Skeleton variant="rounded" width={160} height={40} />
          <Skeleton variant="rounded" width={200} height={40} />
        </Box>
      </Box>

      <Grid container spacing={3} sx={{ mb: 4 }}>
        {[...Array(4)].map((_, i) => (
          <Grid item xs={12} sm={6} md={3} key={i}>
            <Paper sx={{ p: 2 }}>
              <Skeleton variant="text" width={120} />
              <Skeleton variant="text" width={80} height={48} />
            </Paper>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={3}>
        <Grid item xs={12} lg={8}>
          <Paper sx={{ p: 3 }}>
            <Skeleton variant="text" width={220} />
            <Skeleton variant="rounded" height={300} />
          </Paper>
        </Grid>
        <Grid item xs={12} lg={4}>
          <Paper sx={{ p: 3 }}>
            <Skeleton variant="text" width={220} />
            <Skeleton variant="rounded" height={300} />
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );

  const fetchAnalyticsData = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const results = await Promise.allSettled([
        client.get('/analytics/dashboard/summary'),
        client.get(`/analytics/dashboard/timeseries?days=${timeRange}`),
        client.get('/analytics/top-links'),
        client.get(`/analytics/referrers?days=${timeRange}`),
        client.get(`/analytics/devices?days=${timeRange}`),
        client.get(`/analytics/variants?days=${timeRange}`),
        client.get(`/analytics/countries?days=${timeRange}`),
        client.get(`/analytics/sources?days=${timeRange}`)
      ]);

      const allRejected = results.every(r => r.status === 'rejected');

      // Summary
      if (results[0].status === 'fulfilled') {
        setSummaryData(results[0].value.data);
      } else {
        setSummaryData({ totalClicks: 0, totalLinks: 0, activeLinks: 0, inactiveLinks: 0, averageClicksPerLink: 0, mostPopularLink: null });
      }

      // Timeseries
      if (results[1].status === 'fulfilled') {
        setTimeseriesData(results[1].value.data);
      } else {
        setTimeseriesData({ timeseriesData: [], totalClicks: 0, averageDailyClicks: 0, period: `${timeRange} days` });
      }

      // Top links
      if (results[2].status === 'fulfilled') {
        setTopLinksData(results[2].value.data);
      } else {
        setTopLinksData({ topLinks: [] });
      }

      // Referrers
      if (results[3].status === 'fulfilled') {
        setReferrersData(results[3].value.data);
      } else {
        setReferrersData({ referrers: [], period: `${timeRange} days` });
      }

      // Devices
      if (results[4].status === 'fulfilled') {
        setDevicesData(results[4].value.data);
      } else {
        setDevicesData({ devices: [], period: `${timeRange} days` });
      }

      // Variants
      if (results[5].status === 'fulfilled') {
        setVariantsData(results[5].value.data);
      } else {
        setVariantsData({ variants: [], period: `${timeRange} days` });
      }

      // Countries
      if (results[6]?.status === 'fulfilled') {
        setCountriesData(results[6].value.data);
      } else {
        setCountriesData({ countries: [], period: `${timeRange} days` });
      }

      // Sources
      if (results[7]?.status === 'fulfilled') {
        setSourcesData(results[7].value.data);
      } else {
        setSourcesData({ sources: [], period: `${timeRange} days` });
      }

      if (allRejected) {
        setError('Failed to load analytics data. Please try again.');
      }
    } catch (err) {
      console.error('Error fetching analytics data:', err);
      setError('Failed to load analytics data. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [timeRange]);

  useEffect(() => {
    fetchAnalyticsData();
  }, [fetchAnalyticsData]);

  // NOTE: kept only the memoized version to satisfy exhaustive-deps rule

  useEffect(() => {
    const loadPerLink = async () => {
      setPerLinkSeries(null);
      if (!selectedLinkId) return;
      try {
        const res = await client.get(`/analytics/dashboard/timeseries/by-link?linkId=${selectedLinkId}&days=${timeRange}`);
        const all = res.data?.timeseriesData || [];
        setPerLinkSeries(all);
      } catch (e) {
        console.error('Failed to load per-link timeseries', e);
      }
    };
    loadPerLink();
  }, [selectedLinkId, timeRange]);

  useEffect(() => {
    const loadPerLinkVariants = async () => {
      setPerLinkVariantsData(null);
      if (!selectedLinkId) return;
      try {
        const res = await client.get(`/analytics/variants/by-link?linkId=${selectedLinkId}&days=${timeRange}`);
        setPerLinkVariantsData(res.data || { variants: [] });
      } catch (e) {
        console.error('Failed to load per-link variants', e);
        setPerLinkVariantsData({ variants: [] });
      }
    };
    loadPerLinkVariants();
  }, [selectedLinkId, timeRange]);

  useEffect(() => {
    const loadPerLinkSources = async () => {
      setPerLinkSourcesData(null);
      if (!selectedLinkId) return;
      try {
        const res = await client.get(`/analytics/sources/by-link?linkId=${selectedLinkId}&days=${timeRange}`);
        setPerLinkSourcesData(res.data || { sources: [] });
      } catch (e) {
        console.error('Failed to load per-link sources', e);
        setPerLinkSourcesData({ sources: [] });
      }
    };
    loadPerLinkSources();
  }, [selectedLinkId, timeRange]);

  const buildCsv = (rows, columns) => {
    const escape = (v) => {
      if (v == null) return '';
      const s = String(v).replace(/"/g, '""');
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

  // Derived filtered datasets for Sources
  const allSources = (sourcesData?.sources || []);
  const filteredSources = sourceFilter ? allSources.filter(s => (s.source || '').toLowerCase() === sourceFilter.toLowerCase()) : allSources;
  const perLinkAllSources = (perLinkSourcesData?.sources || []);
  const perLinkFilteredSources = sourceFilter ? perLinkAllSources.filter(s => (s.source || '').toLowerCase() === sourceFilter.toLowerCase()) : perLinkAllSources;
  const sourcesChartData = filteredSources.map(s => ({ name: s.source, value: s.clicks }));
  const perLinkSourcesChartData = perLinkFilteredSources.map(s => ({ name: s.source, value: s.clicks }));

  if (loading) {
    return renderLoading();
  }

  if (error) {
    return (
      <Alert
        severity="error"
        sx={{ mb: 2 }}
        action={<Button color="inherit" size="small" onClick={fetchAnalyticsData} aria-label="Retry loading analytics">Retry</Button>}
      >
        {error}
      </Alert>
    );
  }

  const timeseries = (selectedLinkId ? perLinkSeries : timeseriesData?.timeseriesData) || [];
  const hasTimeseries = timeseries.length > 0;
  const topLinks = topLinksData?.topLinks || [];

  const renderNoData = (label = 'No data available') => (
    <Box display="flex" alignItems="center" justifyContent="center" minHeight={160} sx={{ color: 'text.secondary' }}>
      <Typography variant="body2">{label}</Typography>
    </Box>
  );

  return (
    <Box>
      {/* Time Range & Controls (responsive) */}
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ mb: 3, alignItems: { xs: 'stretch', md: 'center' } }}>
        <Typography variant="h4" component="h1" gutterBottom sx={{ flexGrow: 1 }}>
          Analytics Overview
        </Typography>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ width: { xs: '100%', md: 'auto' }, alignItems: { sm: 'center' }, flexWrap: 'wrap' }}>
          <FormControl fullWidth size="small" sx={{ minWidth: { xs: '100%', sm: 140 } }}>
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
          <FormControl fullWidth size="small" sx={{ minWidth: { xs: '100%', sm: 180 } }}>
            <InputLabel>Per-link</InputLabel>
            <Select
              value={selectedLinkId}
              label="Per-link"
              onChange={(e) => setSelectedLinkId(e.target.value)}
            >
              <MenuItem value="">All links</MenuItem>
              {(topLinksData?.topLinks || []).map((l) => (
                <MenuItem key={l.id} value={String(l.id)}>{l.title}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ width: { xs: '100%', sm: 'auto' }, flexWrap: 'wrap' }}>
            <Button
              variant="outlined"
              size="small"
              sx={{ width: { xs: '100%', sm: 'auto' } }}
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
            {selectedLinkId && (
              <Button
                variant="outlined"
                size="small"
                sx={{ width: { xs: '100%', sm: 'auto' } }}
                onClick={() => downloadCsv(
                  `/analytics/export/timeseries/by-link?linkId=${selectedLinkId}&days=${timeRange}`,
                  `analytics_link_${selectedLinkId}_timeseries_${timeRange}d.csv`,
                  (perLinkSeries || []),
                  [
                    ['date', r => r.date],
                    ['clicks', r => r.clicks],
                    ['uniqueVisitors', r => r.uniqueVisitors]
                  ]
                )}
              >
                Export Selected Link Timeseries CSV
              </Button>
            )}
            <Button
              variant="outlined"
              size="small"
              sx={{ width: { xs: '100%', sm: 'auto' } }}
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
          </Stack>
        </Stack>
      </Stack>

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
            {hasTimeseries ? (
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={timeseries}>
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
            ) : renderNoData('No clicks recorded for the selected period')}
          </Paper>
        </Grid>

        {/* Top Links Chart */}
        <Grid item xs={12} lg={4}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Top Performing Links
            </Typography>
            {topLinks.length ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={topLinks}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="title" angle={-45} textAnchor="end" height={80} />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="clickCount" fill="#8884d8" />
                </BarChart>
              </ResponsiveContainer>
            ) : renderNoData('No top links yet')}
          </Paper>
        </Grid>

        {/* Referrers Table */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
              <Typography variant="h6">Top Referrers</Typography>
              <Button size="small" aria-label="Export referrers CSV" onClick={() => downloadCsv(
                `/analytics/export/referrers?days=${timeRange}`,
                `analytics_referrers_${timeRange}d.csv`,
                (referrersData?.referrers || []),
                [
                  ['referrerDomain', r => r.referrerDomain],
                  ['clicks', r => r.clicks],
                  ['uniqueVisitors', r => r.uniqueVisitors]
                ]
              )}>Export CSV</Button>
            </Box>
            <Box sx={{ width: '100%', overflowX: 'auto' }}>
              <Box component="table" sx={{ width: '100%', minWidth: 520, borderCollapse: 'collapse' }}>
              <Box component="thead">
                <Box component="tr">
                  <Box component="th" sx={{ textAlign: 'left', pb: 1 }}>Referrer</Box>
                  <Box component="th" sx={{ textAlign: 'right', pb: 1 }}>Clicks</Box>
                  <Box component="th" sx={{ textAlign: 'right', pb: 1 }}>Uniques</Box>
                </Box>
              </Box>
              <Box component="tbody">
                {(referrersData?.referrers || []).slice(0, 10).map((r, i) => (
                  <Box component="tr" key={i}>
                    <Box component="td" sx={{ py: 0.5 }}>{r.referrerDomain}</Box>
                    <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>{r.clicks}</Box>
                    <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>{r.uniqueVisitors}</Box>
                  </Box>
                ))}
              </Box>
              </Box>
            </Box>
          </Paper>
        </Grid>

        {/* Devices Table */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
              <Typography variant="h6">Devices</Typography>
              <Button size="small" aria-label="Export devices CSV" onClick={() => downloadCsv(
                `/analytics/export/devices?days=${timeRange}`,
                `analytics_devices_${timeRange}d.csv`,
                (devicesData?.devices || []),
                [
                  ['deviceType', r => r.deviceType],
                  ['clicks', r => r.clicks],
                  ['uniqueVisitors', r => r.uniqueVisitors]
                ]
              )}>Export CSV</Button>
            </Box>
            <Box sx={{ width: '100%', overflowX: 'auto' }}>
              <Box component="table" sx={{ width: '100%', minWidth: 520, borderCollapse: 'collapse' }}>
              <Box component="thead">
                <Box component="tr">
                  <Box component="th" sx={{ textAlign: 'left', pb: 1 }}>Device</Box>
                  <Box component="th" sx={{ textAlign: 'right', pb: 1 }}>Clicks</Box>
                  <Box component="th" sx={{ textAlign: 'right', pb: 1 }}>Uniques</Box>
                </Box>
              </Box>
              <Box component="tbody">
                {(devicesData?.devices || []).map((r, i) => (
                  <Box component="tr" key={i}>
                    <Box component="td" sx={{ py: 0.5 }}>{r.deviceType}</Box>
                    <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>{r.clicks}</Box>
                    <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>{r.uniqueVisitors}</Box>
                  </Box>
                ))}
              </Box>
              </Box>
            </Box>
          </Paper>
        </Grid>

        {/* Countries Table */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
              <Typography variant="h6">Countries</Typography>
              <Button size="small" aria-label="Export countries CSV" onClick={() => downloadCsv(
                `/analytics/export/countries?days=${timeRange}`,
                `analytics_countries_${timeRange}d.csv`,
                (countriesData?.countries || []),
                [
                  ['country', r => r.country],
                  ['clicks', r => r.clicks],
                  ['uniqueVisitors', r => r.uniqueVisitors]
                ]
              )}>Export CSV</Button>
            </Box>
            <Box sx={{ width: '100%', overflowX: 'auto' }}>
              <Box component="table" sx={{ width: '100%', minWidth: 520, borderCollapse: 'collapse' }}>
              <Box component="thead">
                <Box component="tr">
                  <Box component="th" sx={{ textAlign: 'left', pb: 1 }}>Country</Box>
                  <Box component="th" sx={{ textAlign: 'right', pb: 1 }}>Clicks</Box>
                  <Box component="th" sx={{ textAlign: 'right', pb: 1 }}>Uniques</Box>
                </Box>
              </Box>
              <Box component="tbody">
                {(countriesData?.countries || []).map((r, i) => (
                  <Box component="tr" key={i}>
                    <Box component="td" sx={{ py: 0.5 }}>{r.country}</Box>
                    <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>{r.clicks}</Box>
                    <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>{r.uniqueVisitors}</Box>
                  </Box>
                ))}
              </Box>
              </Box>
            </Box>
          </Paper>
        </Grid>

        {/* Sources Chart + Table */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
              <Typography variant="h6">Sources</Typography>
              <Button size="small" aria-label="Export sources CSV" onClick={() => downloadCsv(
                `/analytics/export/sources?days=${timeRange}`,
                `analytics_sources_${timeRange}d.csv`,
                (sourcesData?.sources || []),
                [
                  ['source', r => r.source],
                  ['clicks', r => r.clicks],
                  ['uniqueVisitors', r => r.uniqueVisitors]
                ]
              )}>Export CSV</Button>
            </Box>
            <Box display="flex" alignItems="center" gap={2} mb={2}>
              <FormControl size="small" sx={{ minWidth: 160 }}>
                <InputLabel id="src-filter">Filter Source</InputLabel>
                <Select labelId="src-filter" label="Filter Source" value={sourceFilter} onChange={(e) => setSourceFilter(e.target.value)}>
                  <MenuItem value="">All</MenuItem>
                  {[...new Set((sourcesData?.sources || []).map(s => s.source))].map((s) => (
                    <MenuItem key={s} value={s}>{s}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={sourcesChartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" interval={0} angle={-30} textAnchor="end" height={70} />
                <YAxis />
                <Tooltip />
                <Bar dataKey="value" name="Clicks" fill="#00C49F" />
              </BarChart>
            </ResponsiveContainer>
            <Box sx={{ width: '100%', overflowX: 'auto' }}>
              <Box component="table" sx={{ width: '100%', minWidth: 520, borderCollapse: 'collapse' }}>
                <Box component="thead">
                  <Box component="tr">
                    <Box component="th" sx={{ textAlign: 'left', pb: 1 }}>Source</Box>
                    <Box component="th" sx={{ textAlign: 'right', pb: 1 }}>Clicks</Box>
                    <Box component="th" sx={{ textAlign: 'right', pb: 1 }}>Uniques</Box>
                  </Box>
                </Box>
                <Box component="tbody">
                  {filteredSources.map((r, i) => (
                    <Box component="tr" key={i}>
                      <Box component="td" sx={{ py: 0.5 }}>{r.source}</Box>
                      <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>{r.clicks}</Box>
                      <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>{r.uniqueVisitors}</Box>
                    </Box>
                  ))}
                </Box>
              </Box>
            </Box>
          </Paper>
        </Grid>

        {/* Variants Table */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
              <Typography variant="h6">Variants</Typography>
              <Button size="small" aria-label="Export variants CSV" onClick={() => {
                if (!variantsData?.variants) return;
                const csv = buildCsv(variantsData.variants, [
                  ['variantId', r => r.variantId],
                  ['variantTitle', r => r.variantTitle || ''],
                  ['clicks', r => r.clicks],
                  ['uniqueVisitors', r => r.uniqueVisitors]
                ]);
                triggerDownload(csv, `analytics_variants_${timeRange}d.csv`);
              }}>Export CSV</Button>
            </Box>
            <Box sx={{ width: '100%', overflowX: 'auto' }}>
              <Box component="table" sx={{ width: '100%', minWidth: 520, borderCollapse: 'collapse' }}>
                <Box component="thead">
                  <Box component="tr">
                    <Box component="th" sx={{ textAlign: 'left', pb: 1 }}>Variant</Box>
                    <Box component="th" sx={{ textAlign: 'right', pb: 1 }}>Clicks</Box>
                    <Box component="th" sx={{ textAlign: 'right', pb: 1 }}>Uniques</Box>
                  </Box>
                </Box>
                <Box component="tbody">
                  {(variantsData?.variants || []).map((r, i) => (
                    <Box component="tr" key={i}>
                      <Box component="td" sx={{ py: 0.5 }}>{r.variantTitle || `(id ${r.variantId})`}</Box>
                      <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>{r.clicks}</Box>
                      <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>{r.uniqueVisitors}</Box>
                    </Box>
                  ))}
                </Box>
              </Box>
            </Box>
          </Paper>
        </Grid>

        {/* Per-link Variants Table */}
        {selectedLinkId && (
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3 }}>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="h6">Variants for selected link</Typography>
                <Button size="small" aria-label="Export selected link variants CSV" onClick={() => {
                  if (!perLinkVariantsData?.variants) return;
                  downloadCsv(
                    `/analytics/export/variants/by-link?linkId=${selectedLinkId}&days=${timeRange}`,
                    `analytics_link_${selectedLinkId}_variants_${timeRange}d.csv`,
                    perLinkVariantsData.variants,
                    [
                      ['variantId', r => r.variantId],
                      ['variantTitle', r => r.variantTitle || ''],
                      ['clicks', r => r.clicks],
                      ['uniqueVisitors', r => r.uniqueVisitors]
                    ]
                  );
                }}>Export CSV</Button>
              </Box>
              <Box sx={{ width: '100%', overflowX: 'auto' }}>
                <Box component="table" sx={{ width: '100%', minWidth: 520, borderCollapse: 'collapse' }}>
                  <Box component="thead">
                    <Box component="tr">
                      <Box component="th" sx={{ textAlign: 'left', pb: 1 }}>Variant</Box>
                      <Box component="th" sx={{ textAlign: 'right', pb: 1 }}>Clicks</Box>
                      <Box component="th" sx={{ textAlign: 'right', pb: 1 }}>Uniques</Box>
                    </Box>
                  </Box>
                  <Box component="tbody">
                    {(perLinkVariantsData?.variants || []).map((r, i) => (
                      <Box component="tr" key={i}>
                        <Box component="td" sx={{ py: 0.5 }}>{r.variantTitle || `(id ${r.variantId})`}</Box>
                        <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>{r.clicks}</Box>
                        <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>{r.uniqueVisitors}</Box>
                      </Box>
                    ))}
                  </Box>
                </Box>
              </Box>
            </Paper>
          </Grid>
        )}

        {/* Per-link Sources Chart + Table */}
        {selectedLinkId && (
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3 }}>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="h6">Sources for selected link</Typography>
                <Button size="small" aria-label="Export selected link sources CSV" onClick={() => {
                  if (!perLinkSourcesData?.sources) return;
                  downloadCsv(
                    `/analytics/export/sources/by-link?linkId=${selectedLinkId}&days=${timeRange}`,
                    `analytics_link_${selectedLinkId}_sources_${timeRange}d.csv`,
                    perLinkSourcesData.sources,
                    [
                      ['source', r => r.source],
                      ['clicks', r => r.clicks],
                      ['uniqueVisitors', r => r.uniqueVisitors]
                    ]
                  );
                }}>Export CSV</Button>
              </Box>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={perLinkSourcesChartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" interval={0} angle={-30} textAnchor="end" height={70} />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="value" name="Clicks" fill="#0088FE" />
                </BarChart>
              </ResponsiveContainer>
              <Box sx={{ width: '100%', overflowX: 'auto' }}>
                <Box component="table" sx={{ width: '100%', minWidth: 520, borderCollapse: 'collapse' }}>
                  <Box component="thead">
                    <Box component="tr">
                      <Box component="th" sx={{ textAlign: 'left', pb: 1 }}>Source</Box>
                      <Box component="th" sx={{ textAlign: 'right', pb: 1 }}>Clicks</Box>
                      <Box component="th" sx={{ textAlign: 'right', pb: 1 }}>Uniques</Box>
                    </Box>
                  </Box>
                  <Box component="tbody">
                    {perLinkFilteredSources.map((r, i) => (
                      <Box component="tr" key={i}>
                        <Box component="td" sx={{ py: 0.5 }}>{r.source}</Box>
                        <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>{r.clicks}</Box>
                        <Box component="td" sx={{ py: 0.5, textAlign: 'right' }}>{r.uniqueVisitors}</Box>
                      </Box>
                    ))}
                  </Box>
                </Box>
              </Box>
            </Paper>
          </Grid>
        )}

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
