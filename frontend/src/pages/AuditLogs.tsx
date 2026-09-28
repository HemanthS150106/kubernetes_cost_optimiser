import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Paper,
  Typography,
  Box,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
  Alert,
  Snackbar,
} from '@mui/material';
import {
  History as HistoryIcon,
  Restore as RollbackIcon,
} from '@mui/icons-material';
import { managementService } from '../services/api';

export const AuditLogs: React.FC = () => {
  const queryClient = useQueryClient();
  const [rollbackTarget, setRollbackTarget] = useState<any>(null);
  const [feedback, setFeedback] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });

  const { data: logs, isLoading, error } = useQuery({
    queryKey: ['audit-logs'],
    queryFn: managementService.getAuditLogs,
    refetchInterval: 10000, // Poll every 10 seconds
  });

  const rollbackMutation = useMutation({
    mutationFn: (auditLogId: number) => managementService.rollbackRecommendation(auditLogId),
    onSuccess: () => {
      setFeedback({
        open: true,
        message: 'Rollback operation executed successfully.',
        severity: 'success',
      });
      setRollbackTarget(null);
      queryClient.invalidateQueries({ queryKey: ['audit-logs'] });
      queryClient.invalidateQueries({ queryKey: ['cluster-overview'] });
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    },
    onError: (err: any) => {
      setFeedback({
        open: true,
        message: err.response?.data || 'Failed to rollback optimization.',
        severity: 'error',
      });
      setRollbackTarget(null);
    },
  });

  const getActionColor = (action: string) => {
    switch (action) {
      case 'SCALE_DEPLOYMENT':
        return 'info';
      case 'RESTART_DEPLOYMENT':
        return 'primary';
      case 'DELETE_POD':
        return 'error';
      case 'CORDON_NODE':
      case 'DRAIN_NODE':
        return 'warning';
      case 'APPLY_RECOMMENDATION':
        return 'success';
      case 'ROLLBACK_RECOMMENDATION':
        return 'secondary';
      default:
        return 'default';
    }
  };

  const formatConfig = (configJson: string) => {
    if (!configJson || configJson === '{}') return 'N/A';
    try {
      const parsed = JSON.parse(configJson);
      return Object.entries(parsed)
        .map(([key, val]) => `${key}: ${val}`)
        .join(', ');
    } catch {
      return configJson;
    }
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ mt: 3, borderRadius: '12px' }}>
        Failed to fetch cluster transaction logs. Make sure management routes are accessible.
      </Alert>
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: 1 }}>
          <HistoryIcon fontSize="large" color="primary" /> Mutation Audit Ledger
        </Typography>
      </Box>

      <Paper sx={{ width: '100%', overflow: 'hidden', borderRadius: '16px' }}>
        <TableContainer sx={{ maxHeight: '70vh' }}>
          <Table stickyHeader aria-label="audit logs table">
            <TableHead>
              <TableRow>
                <TableCell>Timestamp</TableCell>
                <TableCell>Operator</TableCell>
                <TableCell>Action</TableCell>
                <TableCell>Resource / Namespace</TableCell>
                <TableCell>Previous Spec</TableCell>
                <TableCell>New Spec</TableCell>
                <TableCell align="right">Estimated Cost Impact</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {logs?.length > 0 ? (
                logs.map((log: any) => (
                  <TableRow key={log.id} hover>
                    <TableCell sx={{ fontSize: '0.85rem' }}>
                      {new Date(log.timestamp).toLocaleString()}
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>{log.username}</TableCell>
                    <TableCell>
                      <Chip
                        label={log.actionType.replace('_', ' ')}
                        color={getActionColor(log.actionType) as any}
                        size="small"
                        sx={{ fontWeight: 600, fontSize: '0.75rem' }}
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        {log.resourceName}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Type: {log.resourceType} | NS: {log.namespace}
                      </Typography>
                    </TableCell>
                    <TableCell sx={{ fontSize: '0.8rem', color: 'text.secondary', fontFamily: 'monospace' }}>
                      {formatConfig(log.previousConfiguration)}
                    </TableCell>
                    <TableCell sx={{ fontSize: '0.8rem', color: 'text.secondary', fontFamily: 'monospace' }}>
                      {formatConfig(log.newConfiguration)}
                    </TableCell>
                    <TableCell align="right" sx={{ fontWeight: 600, color: log.estimatedMonthlySavings > 0 ? 'success.main' : log.estimatedMonthlySavings < 0 ? 'error.main' : 'text.primary' }}>
                      {log.estimatedMonthlySavings !== 0 ? `${log.estimatedMonthlySavings > 0 ? '-' : '+'}$${Math.abs(log.estimatedMonthlySavings).toFixed(2)}/mo` : '$0.00'}
                    </TableCell>
                    <TableCell align="center">
                      {log.actionType === 'APPLY_RECOMMENDATION' && (
                        <Button
                          variant="outlined"
                          color="secondary"
                          size="small"
                          startIcon={<RollbackIcon />}
                          onClick={() => setRollbackTarget(log)}
                          sx={{ borderRadius: '8px', textTransform: 'none' }}
                        >
                          Rollback
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 5 }}>
                    <Typography color="text.secondary">No write operations performed yet.</Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Rollback Confirmation Dialog */}
      <Dialog
        open={Boolean(rollbackTarget)}
        onClose={() => setRollbackTarget(null)}
        sx={{ '& .MuiPaper-root': { borderRadius: '16px', p: 1 } }}
      >
        <DialogTitle sx={{ fontWeight: 700 }}>Confirm Optimization Rollback</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Are you sure you want to revert the resource modifications applied to <strong>{rollbackTarget?.resourceName}</strong>?
          </Typography>
          <Alert severity="warning" sx={{ borderRadius: '10px' }}>
            This operation will restore resource parameters to their previous state. The estimated cost savings of <strong>${rollbackTarget?.estimatedMonthlySavings?.toFixed(2)}/month</strong> will be reversed.
          </Alert>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setRollbackTarget(null)} color="inherit" sx={{ textTransform: 'none' }}>
            Cancel
          </Button>
          <Button
            onClick={() => rollbackMutation.mutate(rollbackTarget.id)}
            variant="contained"
            color="secondary"
            disabled={rollbackMutation.isPending}
            startIcon={rollbackMutation.isPending ? <CircularProgress size={16} /> : <RollbackIcon />}
            sx={{ textTransform: 'none', borderRadius: '10px' }}
          >
            Roll back spec
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={feedback.open}
        autoHideDuration={6000}
        onClose={() => setFeedback((f) => ({ ...f, open: false }))}
      >
        <Alert
          onClose={() => setFeedback((f) => ({ ...f, open: false }))}
          severity={feedback.severity}
          sx={{ width: '100%', borderRadius: '12px' }}
        >
          {feedback.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};
