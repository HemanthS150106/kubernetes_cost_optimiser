import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  Button,
  CircularProgress,
  Alert,
  LinearProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Snackbar,
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  Memory as CpuIcon,
  AttachMoney as MoneyIcon,
  Savings as SavingsIcon,
  Layers as PodIcon,
  Dns as NodeIcon,
} from '@mui/icons-material';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as ChartTooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import { clusterService, recommendationService, managementService } from '../services/api';

const COLORS = ['#818cf8', '#34d399', '#f59e0b', '#f43f5e', '#a855f7'];

export const Dashboard: React.FC = () => {
  const queryClient = useQueryClient();
  const [scanMessage, setScanMessage] = useState<string | null>(null);
  const [confirmActionTarget, setConfirmActionTarget] = useState<{ type: 'cordon' | 'uncordon' | 'drain', name: string } | null>(null);

  // Fetch cluster overview details
  const { data: overview, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['clusterOverview'],
    queryFn: clusterService.getOverview,
    refetchInterval: 60000, // Auto refresh every 60s
  });

  // Mutation to cordon/uncordon node
  const cordonMutation = useMutation({
    mutationFn: ({ name, cordon }: { name: string, cordon: boolean }) =>
      managementService.cordonNode(name, cordon),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clusterOverview'] });
      setScanMessage('Node cordon state updated successfully.');
      setTimeout(() => setScanMessage(null), 5000);
      setConfirmActionTarget(null);
    },
    onError: (err: any) => {
      setScanMessage(`Cordon request failed: ${err.response?.data || err.message}`);
      setTimeout(() => setScanMessage(null), 5000);
      setConfirmActionTarget(null);
    },
  });

  // Mutation to drain node
  const drainMutation = useMutation({
    mutationFn: (name: string) => managementService.drainNode(name),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clusterOverview'] });
      setScanMessage('Node drain execution completed successfully.');
      setTimeout(() => setScanMessage(null), 5000);
      setConfirmActionTarget(null);
    },
    onError: (err: any) => {
      setScanMessage(`Drain request failed: ${err.response?.data || err.message}`);
      setTimeout(() => setScanMessage(null), 5000);
      setConfirmActionTarget(null);
    },
  });

  // Mutation to trigger manual scan
  const scanMutation = useMutation({
    mutationFn: recommendationService.triggerScan,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clusterOverview'] });
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
      setScanMessage('Cluster optimization scan finished. New recommendations processed!');
      setTimeout(() => setScanMessage(null), 5000);
    },
    onError: (err: any) => {
      setScanMessage(`Scan failed: ${err.message}`);
    },
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', height: '60vh', alignItems: 'center', justifyContent: 'center' }}>
        <CircularProgress size={50} />
      </Box>
    );
  }

  if (isError) {
    return (
      <Alert severity="error" sx={{ mt: 3, borderRadius: '8px' }}>
        Failed to fetch cluster data: {(error as any).message || 'Error occurred.'}
      </Alert>
    );
  }

  // Safe default initializations
  const cpuCap = overview?.totalCpuCapacity || 0.001;
  const cpuReq = overview?.totalCpuRequest || 0.0;
  const cpuUse = overview?.totalCpuUsage || 0.0;
  const cpuLim = overview?.totalCpuLimit || 0.0;

  const memCap = overview?.totalMemoryCapacityGb || 0.001;
  const memReq = overview?.totalMemoryRequestGb || 0.0;
  const memUse = overview?.totalMemoryUsageGb || 0.0;
  const memLim = overview?.totalMemoryLimitGb || 0.0;

  const kpis = [
    { title: 'Cluster Nodes', value: overview?.totalNodes, subText: 'Active instances', icon: <NodeIcon />, color: '#6366f1' },
    { title: 'Monitored Pods', value: overview?.totalPods, subText: `In ${overview?.totalNamespaces} namespaces`, icon: <PodIcon />, color: '#10b981' },
    { title: 'Current Monthly Cost', value: `$${overview?.currentMonthlyCost?.toFixed(2)}`, subText: 'Node baseline', icon: <MoneyIcon />, color: '#6366f1' },
    { title: 'Potential Savings', value: `$${overview?.potentialMonthlySavings?.toFixed(2)}`, subText: `Optimized cost: $${overview?.optimizedMonthlyCost?.toFixed(2)}`, icon: <SavingsIcon />, color: '#f59e0b' },
  ];

  // Data mapping for charts
  const resourceData = [
    {
      name: 'CPU (Cores)',
      Capacity: parseFloat(cpuCap.toFixed(2)),
      Request: parseFloat(cpuReq.toFixed(2)),
      Limit: parseFloat(cpuLim.toFixed(2)),
      Usage: parseFloat(cpuUse.toFixed(2)),
    },
    {
      name: 'Memory (GB)',
      Capacity: parseFloat(memCap.toFixed(2)),
      Request: parseFloat(memReq.toFixed(2)),
      Limit: parseFloat(memLim.toFixed(2)),
      Usage: parseFloat(memUse.toFixed(2)),
    },
  ];

  const savingsPieData = [
    { name: 'Underutilized CPU', value: parseFloat((cpuReq - cpuUse > 0 ? (cpuReq - cpuUse) * 35.0 : 0.0).toFixed(2)) },
    { name: 'Underutilized Memory', value: parseFloat((memReq - memUse > 0 ? (memReq - memUse) * 4.5 : 0.0).toFixed(2)) },
    { name: 'Idle Node Resources', value: parseFloat((overview?.potentialMonthlySavings ? overview?.potentialMonthlySavings * 0.4 : 0.0).toFixed(2)) },
  ].filter(d => d.value > 0);

  return (
    <Box sx={{ flexGrow: 1 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            Kubernetes Resource & Cost Dashboard
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Analyze resource request-to-utilization waste and monthly cloud expenses.
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => refetch()}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            color="primary"
            onClick={() => scanMutation.mutate()}
            disabled={scanMutation.isPending}
            startIcon={scanMutation.isPending ? <CircularProgress size={16} color="inherit" /> : <SavingsIcon />}
          >
            Scan & Optimize
          </Button>
        </Box>
      </Box>

      {scanMessage && (
        <Alert severity={scanMessage.includes('failed') ? 'error' : 'success'} sx={{ mb: 3, borderRadius: '8px' }}>
          {scanMessage}
        </Alert>
      )}

      {/* KPI Cards Grid */}
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

      {/* Charts Grid */}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '2fr 1fr' }, gap: 3, mb: 4 }}>
        <Box>
          <Paper sx={{ p: 3, borderRadius: '16px' }}>
            <Typography variant="h5" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
              <CpuIcon color="primary" /> Resource Allocation Profiles
            </Typography>
            <Box sx={{ width: '100%', height: 300 }}>
              <ResponsiveContainer>
                <BarChart data={resourceData}>
                  <CartesianGrid strokeDasharray="3 3" opacity={0.1} />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <ChartTooltip />
                  <Legend />
                  <Bar dataKey="Capacity" fill="#818cf8" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="Request" fill="#4f46e5" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="Limit" fill="#a855f7" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="Usage" fill="#34d399" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </Box>
          </Paper>
        </Box>

        <Box>
          <Paper sx={{ p: 3, borderRadius: '16px', height: '100%', display: 'flex', flexDirection: 'column' }}>
            <Typography variant="h5" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
              <SavingsIcon color="secondary" /> Potential Savings Source
            </Typography>
            {savingsPieData.length > 0 ? (
              <Box sx={{ flexGrow: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', height: 230 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={savingsPieData}
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={80}
                      paddingAngle={5}
                      dataKey="value"
                    >
                      {savingsPieData.map((_, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <ChartTooltip formatter={(val) => `$${val}`} />
                  </PieChart>
                </ResponsiveContainer>
              </Box>
            ) : (
              <Box sx={{ display: 'flex', flexGrow: 1, alignItems: 'center', justifyContent: 'center', py: 5 }}>
                <Typography color="text.secondary" variant="body2">
                  No active waste detected in allocations.
                </Typography>
              </Box>
            )}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              {savingsPieData.map((item, idx) => (
                <Box key={idx} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: COLORS[idx % COLORS.length] }} />
                    <Typography variant="body2">{item.name}</Typography>
                  </Box>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    ${item.value.toFixed(2)}
                  </Typography>
                </Box>
              ))}
            </Box>
          </Paper>
        </Box>
      </Box>

      {/* Nodes Status Grid */}
      <Paper sx={{ p: 3, borderRadius: '16px', mb: 4 }}>
        <Typography variant="h5" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
          <NodeIcon color="primary" /> Cluster Node Topology
        </Typography>
        <TableContainer>
          <Table sx={{ minWidth: 650 }}>
            <TableHead>
              <TableRow>
                <TableCell>Node Name</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Role</TableCell>
                <TableCell align="right">CPU Allocatable / Used</TableCell>
                <TableCell align="right">Memory Allocatable / Used</TableCell>
                <TableCell align="center">Pods Running</TableCell>
                <TableCell align="right">Estimated Monthly Cost</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {overview?.nodes?.map((node: any) => {
                const cpuUtil = node.cpuAllocatable > 0 ? (node.cpuUsage / node.cpuAllocatable) * 100 : 0;
                const memUtil = node.memoryAllocatableGb > 0 ? (node.memoryUsageGb / node.memoryAllocatableGb) * 100 : 0;
                return (
                  <TableRow key={node.name} hover>
                    <TableCell sx={{ fontWeight: 600 }}>{node.name}</TableCell>
                    <TableCell>
                      <Chip
                        label={node.status}
                        color={node.status === 'Ready' ? 'success' : 'error'}
                        size="small"
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>
                      <Chip label={node.role} size="small" variant="filled" />
                    </TableCell>
                    <TableCell align="right">
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        {node.cpuAllocatable?.toFixed(2)} Cores / {node.cpuUsage?.toFixed(2)} Cores
                      </Typography>
                      <LinearProgress
                        variant="determinate"
                        value={Math.min(100, cpuUtil)}
                        color={cpuUtil > 80 ? 'error' : cpuUtil > 50 ? 'warning' : 'success'}
                        sx={{ mt: 0.5, borderRadius: 2 }}
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        {node.memoryAllocatableGb?.toFixed(2)} GB / {node.memoryUsageGb?.toFixed(2)} GB
                      </Typography>
                      <LinearProgress
                        variant="determinate"
                        value={Math.min(100, memUtil)}
                        color={memUtil > 80 ? 'error' : memUtil > 50 ? 'warning' : 'success'}
                        sx={{ mt: 0.5, borderRadius: 2 }}
                      />
                    </TableCell>
                    <TableCell align="center">{node.podCount}</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 600 }}>
                      ${node.estimatedMonthlyCost?.toFixed(2)}
                    </TableCell>
                    <TableCell align="center">
                      <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
                        {node.status === 'SchedulingDisabled' ? (
                          <Button
                            variant="outlined"
                            color="success"
                            size="small"
                            onClick={() => setConfirmActionTarget({ type: 'uncordon', name: node.name })}
                            sx={{ textTransform: 'none', borderRadius: '6px' }}
                          >
                            Uncordon
                          </Button>
                        ) : (
                          <Button
                            variant="outlined"
                            color="warning"
                            size="small"
                            onClick={() => setConfirmActionTarget({ type: 'cordon', name: node.name })}
                            sx={{ textTransform: 'none', borderRadius: '6px' }}
                          >
                            Cordon
                          </Button>
                        )}
                        <Button
                          variant="outlined"
                          color="error"
                          size="small"
                          onClick={() => setConfirmActionTarget({ type: 'drain', name: node.name })}
                          sx={{ textTransform: 'none', borderRadius: '6px' }}
                        >
                          Drain
                        </Button>
                      </Box>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Node Action Dialog */}
      <Dialog
        open={Boolean(confirmActionTarget)}
        onClose={() => setConfirmActionTarget(null)}
        sx={{ '& .MuiPaper-root': { borderRadius: '16px', p: 1 } }}
      >
        <DialogTitle sx={{ fontWeight: 700 }}>
          {confirmActionTarget?.type === 'cordon' && 'Cordon Node'}
          {confirmActionTarget?.type === 'uncordon' && 'Uncordon Node'}
          {confirmActionTarget?.type === 'drain' && 'Drain Node'}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            {confirmActionTarget?.type === 'cordon' && `Are you sure you want to cordon ${confirmActionTarget.name}? This stops new pods from scheduling on this node.`}
            {confirmActionTarget?.type === 'uncordon' && `Are you sure you want to uncordon ${confirmActionTarget.name}? This allows new pods to schedule on this node again.`}
            {confirmActionTarget?.type === 'drain' && `WARNING: Draining ${confirmActionTarget.name} will cordon the node and evict all running workloads immediately. This may cause service downtime if workloads do not have sufficient replicas.`}
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setConfirmActionTarget(null)} color="inherit" sx={{ textTransform: 'none' }}>
            Cancel
          </Button>
          <Button
            onClick={() => {
              if (!confirmActionTarget) return;
              if (confirmActionTarget.type === 'cordon') {
                cordonMutation.mutate({ name: confirmActionTarget.name, cordon: true });
              } else if (confirmActionTarget.type === 'uncordon') {
                cordonMutation.mutate({ name: confirmActionTarget.name, cordon: false });
              } else if (confirmActionTarget.type === 'drain') {
                drainMutation.mutate(confirmActionTarget.name);
              }
            }}
            variant="contained"
            color={confirmActionTarget?.type === 'drain' ? 'error' : 'primary'}
            disabled={cordonMutation.isPending || drainMutation.isPending}
            sx={{ textTransform: 'none', borderRadius: '10px' }}
          >
            {confirmActionTarget?.type === 'cordon' && 'Cordon Node'}
            {confirmActionTarget?.type === 'uncordon' && 'Uncordon Node'}
            {confirmActionTarget?.type === 'drain' && 'Evict & Drain Workloads'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={Boolean(scanMessage)}
        autoHideDuration={6000}
        onClose={() => setScanMessage(null)}
      >
        <Alert
          onClose={() => setScanMessage(null)}
          severity={scanMessage?.toLowerCase().includes('failed') ? 'error' : 'success'}
          sx={{ width: '100%', borderRadius: '12px' }}
        >
          {scanMessage}
        </Alert>
      </Snackbar>
    </Box>
  );
};
