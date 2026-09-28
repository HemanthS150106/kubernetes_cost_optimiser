import React from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  CircularProgress,
  Alert,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  LinearProgress,
} from '@mui/material';
import {
  Timeline as TimelineIcon,
  TrendingUp as TrendingUpIcon,
  Speed as SpeedIcon,
  Psychology as PsychologyIcon,
  Insights as InsightsIcon,
  Shield as ShieldIcon,
} from '@mui/icons-material';
import { temporalService } from '../services/api';

/**
 * Temporal Profiles Page — THE NOVELTY VISUALIZATION
 *
 * This page renders the output of the Temporal Anomaly Detection engine:
 * - Summary KPIs: tracked resources, pattern distribution, confidence levels
 * - Per-container temporal profiles with percentile stats, burstiness, and classification
 * - 24-hour CPU usage heatmap for each profiled container
 *
 * This is what differentiates this project from a basic cost optimizer.
 */

const PATTERN_COLORS: Record<string, 'success' | 'warning' | 'error' | 'info' | 'default'> = {
  STABLE: 'success',
  DIURNAL: 'info',
  BURSTY: 'warning',
  GROWING: 'error',
  IDLE: 'default',
};

const PATTERN_DESCRIPTIONS: Record<string, string> = {
  STABLE: 'Flat usage pattern — safe to rightsize aggressively',
  DIURNAL: 'Day/night traffic cycle — rightsize with time-of-day awareness',
  BURSTY: 'Unpredictable spikes — rightsize cautiously with high headroom',
  GROWING: 'Sustained upward trend — do NOT reduce resources',
  IDLE: 'Consistently near-zero — candidate for deletion',
};

// Heatmap color interpolation: green (low) → yellow (mid) → red (high)
function getHeatmapColor(value: number, max: number): string {
  if (max === 0) return 'rgba(99, 102, 241, 0.05)';
  const ratio = Math.min(value / max, 1.0);
  if (ratio < 0.33) {
    return `rgba(16, 185, 129, ${0.15 + ratio * 1.5})`; // Green
  } else if (ratio < 0.66) {
    return `rgba(245, 158, 11, ${0.2 + ratio * 0.8})`; // Amber
  } else {
    return `rgba(239, 68, 68, ${0.3 + ratio * 0.7})`; // Red
  }
}

export const TemporalProfiles: React.FC = () => {
  const { data: profiles, isLoading: profilesLoading, isError: profilesError } = useQuery({
    queryKey: ['temporalProfiles'],
    queryFn: temporalService.getProfiles,
    refetchInterval: 60000,
  });

  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: ['temporalSummary'],
    queryFn: temporalService.getSummary,
    refetchInterval: 60000,
  });

  if (profilesLoading || summaryLoading) {
    return (
      <Box sx={{ display: 'flex', height: '60vh', alignItems: 'center', justifyContent: 'center' }}>
        <CircularProgress size={50} />
      </Box>
    );
  }

  if (profilesError) {
    return (
      <Alert severity="error" sx={{ mt: 3, borderRadius: '8px' }}>
        Failed to load temporal profiles. Ensure the backend has collected sufficient scan data.
      </Alert>
    );
  }

  const kpis = [
    {
      title: 'Tracked Resources',
      value: summary?.trackedResources || 0,
      subText: 'Containers in history window',
      icon: <TimelineIcon />,
      color: '#6366f1',
    },
    {
      title: 'Profiles Available',
      value: summary?.profilesAvailable || 0,
      subText: `${summary?.highConfidenceProfiles || 0} high confidence`,
      icon: <InsightsIcon />,
      color: '#10b981',
    },
    {
      title: 'Avg CPU Burstiness',
      value: `${summary?.avgCpuBurstiness?.toFixed(1) || '—'}x`,
      subText: summary?.avgCpuBurstiness > 3 ? 'Cluster is bursty!' : 'Cluster is stable',
      icon: <SpeedIcon />,
      color: summary?.avgCpuBurstiness > 3 ? '#f59e0b' : '#10b981',
    },
    {
      title: 'Bursty Workloads',
      value: summary?.patternDistribution?.BURSTY || 0,
      subText: 'Spike-sensitive containers',
      icon: <ShieldIcon />,
      color: '#f43f5e',
    },
  ];

  return (
    <Box sx={{ flexGrow: 1 }}>
      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: 1 }}>
          <PsychologyIcon color="primary" /> Temporal Anomaly Profiles
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Time-series-aware workload analysis using P95 percentile statistics and behavioral classification.
          Unlike static-threshold analysis, these profiles account for spikes, diurnal patterns, and burstiness.
        </Typography>
      </Box>

      {/* KPI Cards */}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1fr 1fr 1fr 1fr' }, gap: 3, mb: 4 }}>
        {kpis.map((kpi, idx) => (
          <Box key={idx}>
            <Card sx={{ height: '100%' }}>
              <CardContent sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Box>
                  <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 600 }}>
                    {kpi.title}
                  </Typography>
                  <Typography variant="h4" sx={{ fontWeight: 700, my: 1 }}>
                    {kpi.value}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ fontSize: '0.75rem' }}>
                    {kpi.subText}
                  </Typography>
                </Box>
                <Box
                  sx={{
                    width: 46,
                    height: 46,
                    borderRadius: '12px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    bgcolor: 'action.hover',
                    color: kpi.color,
                  }}
                >
                  {kpi.icon}
                </Box>
              </CardContent>
            </Card>
          </Box>
        ))}
      </Box>

      {/* Pattern Distribution */}
      {summary?.patternDistribution && Object.keys(summary.patternDistribution).length > 0 && (
        <Paper sx={{ p: 3, borderRadius: '16px', mb: 4 }}>
          <Typography variant="h5" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
            <TrendingUpIcon color="primary" /> Workload Pattern Distribution
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            {Object.entries(summary.patternDistribution as Record<string, number>).map(([pattern, count]) => (
              <Tooltip key={pattern} title={PATTERN_DESCRIPTIONS[pattern] || ''} arrow>
                <Chip
                  label={`${pattern}: ${count}`}
                  color={PATTERN_COLORS[pattern] || 'default'}
                  variant="outlined"
                  sx={{ fontWeight: 600, fontSize: '0.9rem', py: 2, px: 1 }}
                />
              </Tooltip>
            ))}
          </Box>
        </Paper>
      )}

      {/* Profiles Table */}
      {profiles && profiles.length > 0 ? (
        <Paper sx={{ p: 3, borderRadius: '16px', mb: 4 }}>
          <Typography variant="h5" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
            <InsightsIcon color="primary" /> Container Temporal Profiles
          </Typography>
          <TableContainer>
            <Table sx={{ minWidth: 900 }}>
              <TableHead>
                <TableRow>
                  <TableCell>Resource</TableCell>
                  <TableCell>Pattern</TableCell>
                  <TableCell align="center">Snapshots</TableCell>
                  <TableCell align="right">CPU P50</TableCell>
                  <TableCell align="right">CPU P95</TableCell>
                  <TableCell align="right">CPU P99</TableCell>
                  <TableCell align="right">CPU Req</TableCell>
                  <TableCell align="center">CPU Burst</TableCell>
                  <TableCell align="right">Mem P95</TableCell>
                  <TableCell align="right">Mem Req</TableCell>
                  <TableCell>24h CPU Heatmap</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {profiles.map((profile: any, idx: number) => {
                  const cpuMaxHourly = profile.hourlyAvgCpu ? Math.max(...profile.hourlyAvgCpu) : 0;
                  const cpuP95Ratio = profile.cpuRequest > 0 ? (profile.cpuP95 / profile.cpuRequest) * 100 : 0;

                  return (
                    <TableRow key={idx} hover>
                      <TableCell sx={{ fontWeight: 600, fontSize: '0.8rem', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {profile.resourceKey}
                      </TableCell>
                      <TableCell>
                        <Tooltip title={PATTERN_DESCRIPTIONS[profile.pattern] || ''} arrow>
                          <Chip
                            label={profile.pattern}
                            color={PATTERN_COLORS[profile.pattern] || 'default'}
                            size="small"
                            variant="outlined"
                          />
                        </Tooltip>
                      </TableCell>
                      <TableCell align="center">
                        <Tooltip title={profile.snapshotCount >= 12 ? 'High confidence' : 'Low confidence — needs more scans'} arrow>
                          <Chip
                            label={profile.snapshotCount}
                            size="small"
                            color={profile.snapshotCount >= 12 ? 'success' : 'warning'}
                            variant="filled"
                          />
                        </Tooltip>
                      </TableCell>
                      <TableCell align="right">{profile.cpuP50?.toFixed(3)}</TableCell>
                      <TableCell align="right">
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {profile.cpuP95?.toFixed(3)}
                        </Typography>
                        <LinearProgress
                          variant="determinate"
                          value={Math.min(100, cpuP95Ratio)}
                          color={cpuP95Ratio > 80 ? 'error' : cpuP95Ratio > 50 ? 'warning' : 'success'}
                          sx={{ mt: 0.5, borderRadius: 2, height: 4 }}
                        />
                      </TableCell>
                      <TableCell align="right">{profile.cpuP99?.toFixed(3)}</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 500 }}>{profile.cpuRequest?.toFixed(3)}</TableCell>
                      <TableCell align="center">
                        <Chip
                          label={`${profile.cpuBurstiness?.toFixed(1)}x`}
                          size="small"
                          color={profile.cpuBurstiness > 3 ? 'error' : profile.cpuBurstiness > 1.5 ? 'warning' : 'success'}
                          variant="filled"
                        />
                      </TableCell>
                      <TableCell align="right">{profile.memP95?.toFixed(3)} GB</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 500 }}>{profile.memRequestGb?.toFixed(3)} GB</TableCell>
                      <TableCell>
                        {/* 24-hour CPU Heatmap */}
                        <Tooltip
                          title={
                            <Box>
                              <Typography variant="caption" sx={{ fontWeight: 700 }}>24h CPU Usage Heatmap</Typography>
                              {profile.hourlyAvgCpu?.map((val: number, h: number) => (
                                <Typography key={h} variant="caption" sx={{ display: 'block' }}>
                                  {String(h).padStart(2, '0')}:00 → {val.toFixed(4)} cores
                                </Typography>
                              ))}
                            </Box>
                          }
                          arrow
                        >
                          <Box sx={{ display: 'flex', gap: '1px', alignItems: 'center' }}>
                            {profile.hourlyAvgCpu?.map((val: number, h: number) => (
                              <Box
                                key={h}
                                sx={{
                                  width: 8,
                                  height: 20,
                                  borderRadius: '2px',
                                  backgroundColor: getHeatmapColor(val, cpuMaxHourly),
                                  transition: 'all 0.2s ease',
                                  '&:hover': {
                                    transform: 'scaleY(1.5)',
                                    zIndex: 1,
                                  },
                                }}
                              />
                            ))}
                          </Box>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      ) : (
        <Paper sx={{ p: 5, borderRadius: '16px', textAlign: 'center' }}>
          <PsychologyIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            No Temporal Profiles Available Yet
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            The system needs at least 6 scan intervals (≈30 minutes at 5-minute intervals) to build
            statistically reliable temporal profiles. Run a few scans from the Dashboard and check back.
          </Typography>
        </Paper>
      )}

      {/* How It Works Explainer */}
      <Paper sx={{ p: 3, borderRadius: '16px', mt: 4, bgcolor: 'action.hover' }}>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>
          ⚡ How Temporal Anomaly Detection Works
        </Typography>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr 1fr' }, gap: 3 }}>
          <Box>
            <Typography variant="subtitle2" color="primary" sx={{ fontWeight: 700 }}>1. Collect</Typography>
            <Typography variant="body2" color="text.secondary">
              Every 5-minute scan records per-container CPU and memory snapshots into a 24-hour sliding window.
            </Typography>
          </Box>
          <Box>
            <Typography variant="subtitle2" color="primary" sx={{ fontWeight: 700 }}>2. Profile</Typography>
            <Typography variant="body2" color="text.secondary">
              After 6+ snapshots, the engine computes P50/P95/P99 percentiles, burstiness coefficients,
              and classifies workload patterns (STABLE, DIURNAL, BURSTY, GROWING, IDLE).
            </Typography>
          </Box>
          <Box>
            <Typography variant="subtitle2" color="primary" sx={{ fontWeight: 700 }}>3. Recommend</Typography>
            <Typography variant="body2" color="text.secondary">
              Unlike static rules, recommendations use P95 usage with pattern-aware safety headroom
              (30% for stable, 80% for bursty), eliminating false-positive rightsizing.
            </Typography>
          </Box>
        </Box>
      </Paper>
    </Box>
  );
};
