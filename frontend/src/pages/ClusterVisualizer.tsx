import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  CircularProgress,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  LinearProgress,
  Tooltip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Divider,
} from '@mui/material';
import {
  ReportProblem as WarnIcon,
  CheckCircle as ValidIcon,
} from '@mui/icons-material';
import { clusterService, managementService } from '../services/api';

export const ClusterVisualizer: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedNamespace, setSelectedNamespace] = useState('all');
  const [confirmDeletePod, setConfirmDeletePod] = useState<{ name: string, namespace: string } | null>(null);
  const [confirmScale, setConfirmScale] = useState<{ name: string, namespace: string, currentReplicas: number } | null>(null);
  const [confirmRestart, setConfirmRestart] = useState<{ name: string, namespace: string } | null>(null);
  const [scaleValue, setScaleValue] = useState<number>(1);
  const [visualizerMessage, setVisualizerMessage] = useState<string | null>(null);

  const { data: overview, isLoading, isError, error } = useQuery({
    queryKey: ['clusterOverview'],
    queryFn: clusterService.getOverview,
  });

  const { data: namespaces } = useQuery({
    queryKey: ['namespaces'],
    queryFn: clusterService.getNamespaces,
  });

  // Pod deletion mutation
  const deletePodMutation = useMutation({
    mutationFn: ({ name, namespace }: { name: string, namespace: string }) =>
      managementService.deletePod(namespace, name),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clusterOverview'] });
      setConfirmDeletePod(null);
      setVisualizerMessage('Pod deletion requested successfully.');
      setTimeout(() => setVisualizerMessage(null), 5000);
    },
    onError: (err: any) => {
      setVisualizerMessage(`Failed to delete pod: ${err.response?.data || err.message}`);
      setTimeout(() => setVisualizerMessage(null), 5000);
      setConfirmDeletePod(null);
    },
  });

  // Scale deployment mutation
  const scaleMutation = useMutation({
    mutationFn: ({ name, namespace, replicas }: { name: string, namespace: string, replicas: number }) =>
      managementService.scaleDeployment(namespace, name, replicas),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clusterOverview'] });
      setConfirmScale(null);
      setVisualizerMessage('Scale command updated successfully.');
      setTimeout(() => setVisualizerMessage(null), 5000);
    },
    onError: (err: any) => {
      setVisualizerMessage(`Failed to scale: ${err.response?.data || err.message}`);
      setTimeout(() => setVisualizerMessage(null), 5000);
      setConfirmScale(null);
    },
  });

  // Restart deployment mutation
  const restartMutation = useMutation({
    mutationFn: ({ name, namespace }: { name: string, namespace: string }) =>
      managementService.restartDeployment(namespace, name),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clusterOverview'] });
      setConfirmRestart(null);
      setVisualizerMessage('Rolling restart rollout initiated successfully.');
      setTimeout(() => setVisualizerMessage(null), 5000);
    },
    onError: (err: any) => {
      setVisualizerMessage(`Failed to restart: ${err.response?.data || err.message}`);
      setTimeout(() => setVisualizerMessage(null), 5000);
      setConfirmRestart(null);
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
        Failed to load cluster details: {(error as any).message || 'Error occurred.'}
      </Alert>
    );
  }

  const pods = overview?.pods || [];
  
  // Filter pods by namespace
  const filteredPods = selectedNamespace === 'all'
    ? pods
    : pods.filter((p: any) => p.namespace === selectedNamespace);

  // Group pods by namespace for visual structure
  const groupedPods = filteredPods.reduce((acc: any, pod: any) => {
    if (!acc[pod.namespace]) acc[pod.namespace] = [];
    acc[pod.namespace].push(pod);
    return acc;
  }, {});

  // Determine pod state health:
  // - RED if missing request/limit
  // - ORANGE if huge request-to-usage gap (> 70% waste)
  // - GREEN if healthy
  const getPodHealth = (pod: any) => {
    let missingReqOrLimit = false;
    let totalReq = 0.0;
    let totalUse = 0.0;

    pod.containers.forEach((c: any) => {
      if (!c.cpuRequest || !c.cpuLimit || !c.memoryRequestGb || !c.memoryLimitGb) {
        missingReqOrLimit = true;
      }
      totalReq += (c.cpuRequest || 0) + (c.memoryRequestGb || 0);
      totalUse += (c.cpuUsage || 0) + (c.memoryUsageGb || 0);
    });

    if (missingReqOrLimit) {
      return { status: 'MISSING_SPEC', color: '#ef4444', label: 'Missing Limits/Requests', icon: <WarnIcon sx={{ color: '#ef4444' }} /> };
    }

    if (totalReq > 0 && (totalUse / totalReq) < 0.25) {
      return { status: 'OVERPROVISIONED', color: '#f59e0b', label: 'Overprovisioned', icon: <WarnIcon sx={{ color: '#f59e0b' }} /> };
    }

    return { status: 'OPTIMAL', color: '#10b981', label: 'Optimal', icon: <ValidIcon sx={{ color: '#10b981' }} /> };
  };

  return (
    <Box sx={{ flexGrow: 1 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            Cluster Resource Tree
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Visual map of pods resource status across cluster namespaces.
          </Typography>
        </Box>
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel>Namespace</InputLabel>
          <Select
            value={selectedNamespace}
            label="Namespace"
            onChange={(e) => setSelectedNamespace(e.target.value)}
          >
            <MenuItem value="all">All Namespaces</MenuItem>
            {namespaces?.map((ns: string) => (
              <MenuItem key={ns} value={ns}>{ns}</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      {visualizerMessage && (
        <Alert severity="info" sx={{ mb: 3, borderRadius: '8px' }}>
          {visualizerMessage}
        </Alert>
      )}

      {/* Legend */}
      <Paper sx={{ p: 2, mb: 4, display: 'flex', gap: 4, justifyContent: 'center' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <ValidIcon sx={{ color: '#10b981' }} />
          <Typography variant="body2">Optimal (Usage &gt;= 25% of request)</Typography>
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <WarnIcon sx={{ color: '#f59e0b' }} />
          <Typography variant="body2">Overprovisioned (Usage &lt; 25% of request)</Typography>
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <WarnIcon sx={{ color: '#ef4444' }} />
          <Typography variant="body2">Missing requests or limits spec</Typography>
        </Box>
      </Paper>

      {Object.keys(groupedPods).length === 0 ? (
        <Alert severity="info" sx={{ borderRadius: '8px' }}>
          No workloads found in selected filter namespace.
        </Alert>
      ) : (
        Object.entries(groupedPods).map(([namespace, nspods]: [string, any]) => (
          <Box key={namespace} sx={{ mb: 5 }}>
            <Typography variant="h5" sx={{ mb: 2, fontWeight: 700, borderBottom: 1, borderColor: 'divider', pb: 1 }}>
              Namespace: {namespace}
            </Typography>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1fr 1fr 1fr' }, gap: 3 }}>
              {nspods.map((pod: any) => {
                const health = getPodHealth(pod);
                const cpuUse = pod.containers.reduce((acc: number, c: any) => acc + (c.cpuUsage || 0), 0);
                const cpuReq = pod.containers.reduce((acc: number, c: any) => acc + (c.cpuRequest || 0), 0);
                const memUse = pod.containers.reduce((acc: number, c: any) => acc + (c.memoryUsageGb || 0), 0);
                const memReq = pod.containers.reduce((acc: number, c: any) => acc + (c.memoryRequestGb || 0), 0);

                const cpuUtil = cpuReq > 0 ? (cpuUse / cpuReq) * 100 : 0;
                const memUtil = memReq > 0 ? (memUse / memReq) * 100 : 0;

                return (
                  <Box key={pod.name}>
                    <Tooltip
                      title={
                        <Box sx={{ p: 1 }}>
                          <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>Container Details</Typography>
                          {pod.containers.map((c: any, i: number) => (
                            <Box key={i} sx={{ my: 0.5 }}>
                              <Typography variant="body2" color="inherit"><strong>{c.name}</strong></Typography>
                              <Typography variant="caption" sx={{ display: 'block' }}>CPU Req/Limit/Usage: {c.cpuRequest?.toFixed(2)}/{c.cpuLimit?.toFixed(2)}/{c.cpuUsage?.toFixed(2)} cores</Typography>
                              <Typography variant="caption" sx={{ display: 'block' }}>Mem Req/Limit/Usage: {c.memoryRequestGb?.toFixed(2)}/{c.memoryLimitGb?.toFixed(2)}/{c.memoryUsageGb?.toFixed(2)} GB</Typography>
                            </Box>
                          ))}
                        </Box>
                      }
                      arrow
                    >
                      <Card
                        sx={{
                          borderLeft: 6,
                          borderColor: health.color,
                          height: '100%',
                        }}
                      >
                        <CardContent>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                            <Box sx={{ overflow: 'hidden', mr: 1 }}>
                              <Typography variant="subtitle1" noWrap sx={{ fontWeight: 700, fontSize: '0.95rem' }}>
                                {pod.name}
                              </Typography>
                              <Typography variant="body2" color="text.secondary" sx={{ fontSize: '0.75rem' }}>
                                Controller: {pod.controllerType} ({pod.controllerName})
                              </Typography>
                            </Box>
                            <Box>{health.icon}</Box>
                          </Box>

                          <Box sx={{ mb: 1.5 }}>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                              <Typography variant="body2" color="text.secondary" sx={{ fontSize: '0.8rem' }}>CPU Usage vs Request</Typography>
                              <Typography variant="body2" sx={{ fontSize: '0.8rem', fontWeight: 600 }}>
                                {cpuUse.toFixed(2)} / {cpuReq > 0 ? cpuReq.toFixed(2) : '-'} cores
                              </Typography>
                            </Box>
                            <LinearProgress
                              variant="determinate"
                              value={cpuReq > 0 ? Math.min(100, cpuUtil) : 0}
                              color={cpuReq > 0 ? (cpuUtil < 25 ? 'warning' : 'success') : 'error'}
                              sx={{ height: 6, borderRadius: 3 }}
                            />
                          </Box>

                          <Box>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                              <Typography variant="body2" color="text.secondary" sx={{ fontSize: '0.8rem' }}>Memory Usage vs Request</Typography>
                              <Typography variant="body2" sx={{ fontSize: '0.8rem', fontWeight: 600 }}>
                                {memUse.toFixed(2)} / {memReq > 0 ? memReq.toFixed(2) : '-'} GB
                              </Typography>
                            </Box>
                            <LinearProgress
                              variant="determinate"
                              value={memReq > 0 ? Math.min(100, memUtil) : 0}
                              color={memReq > 0 ? (memUtil < 25 ? 'warning' : 'success') : 'error'}
                              sx={{ height: 6, borderRadius: 3 }}
                            />
                          </Box>

                          <Divider sx={{ my: 1.5 }} />
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <Button
                              variant="outlined"
                              color="error"
                              size="small"
                              onClick={(e) => {
                                e.stopPropagation();
                                setConfirmDeletePod({ name: pod.name, namespace: pod.namespace });
                              }}
                              sx={{ textTransform: 'none', py: 0.2, px: 1, fontSize: '0.75rem', borderRadius: '6px' }}
                            >
                              Delete Pod
                            </Button>
                            
                            {pod.controllerType === 'Deployment' && (
                              <Box sx={{ display: 'flex', gap: 0.5 }}>
                                <Button
                                  variant="outlined"
                                  color="info"
                                  size="small"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    setScaleValue(pod.containers[0]?.replicas || 1);
                                    setConfirmScale({ name: pod.controllerName, namespace: pod.namespace, currentReplicas: 1 });
                                  }}
                                  sx={{ textTransform: 'none', py: 0.2, px: 1, fontSize: '0.75rem', borderRadius: '6px' }}
                                >
                                  Scale
                                </Button>
                                <Button
                                  variant="outlined"
                                  color="primary"
                                  size="small"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    setConfirmRestart({ name: pod.controllerName, namespace: pod.namespace });
                                  }}
                                  sx={{ textTransform: 'none', py: 0.2, px: 1, fontSize: '0.75rem', borderRadius: '6px' }}
                                >
                                  Restart
                                </Button>
                              </Box>
                            )}
                          </Box>
                        </CardContent>
                      </Card>
                    </Tooltip>
                  </Box>
                );
              })}
            </Box>
          </Box>
        ))
      )}
      {/* Delete Pod Confirmation Dialog */}
      <Dialog
        open={Boolean(confirmDeletePod)}
        onClose={() => setConfirmDeletePod(null)}
        sx={{ '& .MuiPaper-root': { borderRadius: '16px', p: 1 } }}
      >
        <DialogTitle sx={{ fontWeight: 700 }}>Delete Kubernetes Pod</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Are you sure you want to delete pod <strong>{confirmDeletePod?.name}</strong> in namespace <strong>{confirmDeletePod?.namespace}</strong>?
          </Typography>
          <Alert severity="warning" sx={{ borderRadius: '10px' }}>
            This will immediately evict and terminate the pod. If managed by a controller, the cluster scheduler will spin up a fresh pod replica.
          </Alert>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setConfirmDeletePod(null)} color="inherit" sx={{ textTransform: 'none' }}>
            Cancel
          </Button>
          <Button
            onClick={() => {
              if (confirmDeletePod) {
                deletePodMutation.mutate({ name: confirmDeletePod.name, namespace: confirmDeletePod.namespace });
              }
            }}
            variant="contained"
            color="error"
            disabled={deletePodMutation.isPending}
            sx={{ textTransform: 'none', borderRadius: '10px' }}
          >
            Terminate Pod
          </Button>
        </DialogActions>
      </Dialog>

      {/* Scale Deployment Dialog */}
      <Dialog
        open={Boolean(confirmScale)}
        onClose={() => setConfirmScale(null)}
        sx={{ '& .MuiPaper-root': { borderRadius: '16px', p: 1, minWidth: 320 } }}
      >
        <DialogTitle sx={{ fontWeight: 700 }}>Scale Deployment Replicas</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Specify the replica count for deployment <strong>{confirmScale?.name}</strong> in namespace <strong>{confirmScale?.namespace}</strong>:
          </Typography>
          <TextField
            fullWidth
            type="number"
            label="Replicas"
            variant="outlined"
            value={scaleValue}
            onChange={(e: any) => setScaleValue(Math.max(0, parseInt(e.target.value) || 0))}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setConfirmScale(null)} color="inherit" sx={{ textTransform: 'none' }}>
            Cancel
          </Button>
          <Button
            onClick={() => {
              if (confirmScale) {
                scaleMutation.mutate({ name: confirmScale.name, namespace: confirmScale.namespace, replicas: scaleValue });
              }
            }}
            variant="contained"
            color="primary"
            disabled={scaleMutation.isPending}
            sx={{ textTransform: 'none', borderRadius: '10px' }}
          >
            Apply replica count
          </Button>
        </DialogActions>
      </Dialog>

      {/* Restart Deployment Dialog */}
      <Dialog
        open={Boolean(confirmRestart)}
        onClose={() => setConfirmRestart(null)}
        sx={{ '& .MuiPaper-root': { borderRadius: '16px', p: 1 } }}
      >
        <DialogTitle sx={{ fontWeight: 700 }}>Rolling restart rollout</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Are you sure you want to trigger a rolling restart for deployment <strong>{confirmRestart?.name}</strong> in namespace <strong>{confirmRestart?.namespace}</strong>?
          </Typography>
          <Alert severity="info" sx={{ borderRadius: '10px' }}>
            This patches the deployment spec to trigger Kubernetes pod recreations sequentially, ensuring zero-downtime rollouts.
          </Alert>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setConfirmRestart(null)} color="inherit" sx={{ textTransform: 'none' }}>
            Cancel
          </Button>
          <Button
            onClick={() => {
              if (confirmRestart) {
                restartMutation.mutate({ name: confirmRestart.name, namespace: confirmRestart.namespace });
              }
            }}
            variant="contained"
            color="primary"
            disabled={restartMutation.isPending}
            sx={{ textTransform: 'none', borderRadius: '10px' }}
          >
            Trigger restart
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
