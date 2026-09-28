import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Paper,
  Typography,
  Box,
  CircularProgress,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Card,
  CardContent,
  IconButton,
  Tooltip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import {
  CheckCircleOutlined as DoneIcon,
  HighlightOff as DismissIcon,
  DeleteOutlined as DeleteIcon,
  FilterList as FilterIcon,
  InfoOutlined as InfoIcon,
} from '@mui/icons-material';
import { recommendationService, clusterService, managementService } from '../services/api';

export const Recommendations: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedNamespace, setSelectedNamespace] = useState('all');
  const [selectedStatus, setSelectedStatus] = useState('ACTIVE');
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [confirmApplyTarget, setConfirmApplyTarget] = useState<any | null>(null);

  // Fetch recommendations list
  const { data: recommendations, isLoading, isError, error } = useQuery({
    queryKey: ['recommendations', selectedStatus, selectedNamespace],
    queryFn: () => recommendationService.getRecommendations(selectedStatus, selectedNamespace),
  });

  // Fetch namespaces for filtering
  const { data: namespaces } = useQuery({
    queryKey: ['namespaces'],
    queryFn: clusterService.getNamespaces,
  });

  // Mutation to apply a recommendation to the cluster in real time
  const applyMutation = useMutation({
    mutationFn: (id: number) => managementService.applyRecommendation(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
      queryClient.invalidateQueries({ queryKey: ['clusterOverview'] });
      setActionMessage('Recommendation applied to the cluster in real time.');
      setTimeout(() => setActionMessage(null), 4000);
      setConfirmApplyTarget(null);
    },
    onError: (err: any) => {
      setActionMessage(`Failed to apply optimization: ${err.response?.data || err.message}`);
      setTimeout(() => setActionMessage(null), 5000);
      setConfirmApplyTarget(null);
    },
  });

  // Mutation to update status (e.g. IMPLEMENTED or DISMISSED)
  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      recommendationService.updateStatus(id, status),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
      queryClient.invalidateQueries({ queryKey: ['clusterOverview'] });
      setActionMessage(`Recommendation marked as ${variables.status.toLowerCase()}.`);
      setTimeout(() => setActionMessage(null), 4000);
    },
  });

  // Mutation to delete a recommendation record
  const deleteMutation = useMutation({
    mutationFn: recommendationService.deleteRecommendation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
      queryClient.invalidateQueries({ queryKey: ['clusterOverview'] });
      setActionMessage('Recommendation entry removed successfully.');
      setTimeout(() => setActionMessage(null), 4000);
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
        Failed to fetch recommendations: {(error as any).message || 'Error occurred.'}
      </Alert>
    );
  }

  const getSeverityColor = (sev: string) => {
    switch (sev) {
      case 'CRITICAL': return 'error';
      case 'HIGH': return 'warning';
      case 'MEDIUM': return 'info';
      default: return 'default';
    }
  };

  const getRecommendationBadge = (type: string) => {
    if (type.startsWith('ADD_')) return 'primary';
    if (type.startsWith('REDUCE_')) return 'secondary';
    return 'default';
  };

  return (
    <Box sx={{ flexGrow: 1 }}>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Optimization Recommendations
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Review and execute operations to eliminate overprovisioning and secure container configurations.
        </Typography>
      </Box>

      {actionMessage && (
        <Alert severity="info" sx={{ mb: 3, borderRadius: '8px' }}>
          {actionMessage}
        </Alert>
      )}

      {/* Filter Options */}
      <Card sx={{ mb: 4 }}>
        <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 3, py: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <FilterIcon color="action" />
            <Typography variant="subtitle2">Filters:</Typography>
          </Box>

          <FormControl size="small" sx={{ minWidth: 160 }}>
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

          <FormControl size="small" sx={{ minWidth: 160 }}>
            <InputLabel>Status</InputLabel>
            <Select
              value={selectedStatus}
              label="Status"
              onChange={(e) => setSelectedStatus(e.target.value)}
            >
              <MenuItem value="ACTIVE">Active Opportunities</MenuItem>
              <MenuItem value="IMPLEMENTED">Implemented</MenuItem>
              <MenuItem value="DISMISSED">Dismissed</MenuItem>
            </Select>
          </FormControl>
        </CardContent>
      </Card>

      {/* Recommendations Table */}
      <Paper sx={{ borderRadius: '16px', overflow: 'hidden' }}>
        <TableContainer>
          <Table sx={{ minWidth: 650 }}>
            <TableHead>
              <TableRow>
                <TableCell>Resource Name</TableCell>
                <TableCell>Namespace</TableCell>
                <TableCell>Recommendation Type</TableCell>
                <TableCell>Severity</TableCell>
                <TableCell align="right">Current Spec</TableCell>
                <TableCell align="right">Recommended Spec</TableCell>
                <TableCell align="right">Est. Monthly Savings</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {recommendations && recommendations.length > 0 ? (
                recommendations.map((rec: any) => (
                  <TableRow key={rec.id} hover>
                    <TableCell>
                      <Box>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {rec.resourceName.split('/')[0]}
                        </Typography>
                        {rec.resourceName.includes('/') && (
                          <Typography variant="caption" color="text.secondary">
                            Container: {rec.resourceName.split('/')[1]}
                          </Typography>
                        )}
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 0.5 }}>
                          <InfoIcon sx={{ fontSize: '0.9rem', color: 'text.secondary' }} />
                          <Typography variant="caption" color="text.secondary">
                            {rec.details}
                          </Typography>
                        </Box>
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip label={rec.namespace} size="small" variant="outlined" />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={rec.type.replace(/_/g, ' ')}
                        color={getRecommendationBadge(rec.type)}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={rec.severity}
                        color={getSeverityColor(rec.severity)}
                        size="small"
                      />
                    </TableCell>
                    <TableCell align="right">
                      {rec.currentCpuRequest ? `${rec.currentCpuRequest.toFixed(2)} cores` : '-'}
                      {rec.currentMemoryRequestGb ? ` / ${rec.currentMemoryRequestGb.toFixed(2)} GB` : ''}
                      {rec.currentReplicas ? `${rec.currentReplicas} replicas` : ''}
                    </TableCell>
                    <TableCell align="right">
                      {rec.recommendedCpuRequest ? `${rec.recommendedCpuRequest.toFixed(2)} cores` : '-'}
                      {rec.recommendedMemoryRequestGb ? ` / ${rec.recommendedMemoryRequestGb.toFixed(2)} GB` : ''}
                      {rec.recommendedReplicas ? `${rec.recommendedReplicas} replicas` : ''}
                    </TableCell>
                    <TableCell align="right" sx={{ fontWeight: 700, color: 'secondary.main' }}>
                      {rec.estimatedMonthlySavings > 0 ? `$${rec.estimatedMonthlySavings.toFixed(2)}` : '$0.00'}
                    </TableCell>
                    <TableCell align="center">
                      <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
                        {rec.status === 'ACTIVE' && (
                          <>
                            <Tooltip title="Apply Action">
                              <IconButton
                                color="success"
                                size="small"
                                onClick={() => setConfirmApplyTarget(rec)}
                                disabled={applyMutation.isPending}
                              >
                                <DoneIcon />
                              </IconButton>
                            </Tooltip>
                            <Tooltip title="Dismiss Action">
                              <IconButton
                                color="warning"
                                size="small"
                                onClick={() => statusMutation.mutate({ id: rec.id, status: 'DISMISSED' })}
                                disabled={statusMutation.isPending}
                              >
                                <DismissIcon />
                              </IconButton>
                            </Tooltip>
                          </>
                        )}
                        <Tooltip title="Delete record">
                          <IconButton
                            color="error"
                            size="small"
                            onClick={() => deleteMutation.mutate(rec.id)}
                            disabled={deleteMutation.isPending}
                          >
                            <DeleteIcon />
                          </IconButton>
                        </Tooltip>
                      </Box>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 6 }}>
                    <Typography color="text.secondary">
                      No cost optimization recommendations found matching filters.
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
      {/* Apply Confirmation Dialog */}
      <Dialog
        open={Boolean(confirmApplyTarget)}
        onClose={() => setConfirmApplyTarget(null)}
        sx={{ '& .MuiPaper-root': { borderRadius: '16px', p: 1 } }}
      >
        <DialogTitle sx={{ fontWeight: 700 }}>Apply Resource Optimization</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Are you sure you want to execute this change on the cluster?
          </Typography>
          <Typography variant="body2" sx={{ mb: 1 }}>
            <strong>Resource:</strong> {confirmApplyTarget?.resourceName}
          </Typography>
          <Typography variant="body2" sx={{ mb: 1 }}>
            <strong>Namespace:</strong> {confirmApplyTarget?.namespace}
          </Typography>
          <Typography variant="body2" sx={{ mb: 2 }}>
            <strong>Type:</strong> {confirmApplyTarget?.type?.replace(/_/g, ' ')}
          </Typography>
          <Alert severity="info" sx={{ borderRadius: '10px' }}>
            Applying this optimization will update specifications in real-time, resulting in an estimated monthly savings of <strong>${confirmApplyTarget?.estimatedMonthlySavings?.toFixed(2)}/month</strong>.
          </Alert>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setConfirmApplyTarget(null)} color="inherit" sx={{ textTransform: 'none' }}>
            Cancel
          </Button>
          <Button
            onClick={() => applyMutation.mutate(confirmApplyTarget.id)}
            variant="contained"
            color="success"
            disabled={applyMutation.isPending}
            startIcon={applyMutation.isPending ? <CircularProgress size={16} /> : <DoneIcon />}
            sx={{ textTransform: 'none', borderRadius: '10px' }}
          >
            Apply spec change
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
