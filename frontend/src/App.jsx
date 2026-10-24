import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './components/AppLayout';

import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import ComplaintsListPage from './pages/ComplaintsListPage';
import ComplaintDetailPage from './pages/ComplaintDetailPage';
import SubmitComplaintPage from './pages/SubmitComplaintPage';
import NotificationsPage from './pages/NotificationsPage';
import AdminOfficersPage from './pages/AdminOfficersPage';

function UnauthorizedPage() {
  return (
    <div className="flex flex-col items-center justify-center h-64 gap-3 text-white/30">
      <div className="text-5xl">🚫</div>
      <h2 className="text-lg font-semibold text-white/60">Access Denied</h2>
      <p className="text-sm">You don&apos;t have permission to view this page.</p>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/unauthorized" element={<UnauthorizedPage />} />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />

          {/* Protected layout routes */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/complaints/submit" element={<SubmitComplaintPage />} />
              <Route path="/complaints/:id" element={<ComplaintDetailPage />} />
              <Route path="/complaints/my" element={<ComplaintsListPage isMyComplaints={true} />} />
              <Route path="/notifications" element={<NotificationsPage />} />

              {/* Officer + Admin only */}
              <Route element={<ProtectedRoute requiredRoles={['OFFICER', 'ADMIN']} />}>
                <Route path="/complaints/all" element={<ComplaintsListPage isMyComplaints={false} />} />
              </Route>

              {/* Admin only */}
              <Route element={<ProtectedRoute requiredRoles={['ADMIN']} />}>
                <Route path="/admin/officers" element={<AdminOfficersPage />} />
              </Route>
            </Route>
          </Route>

          {/* 404 fallback */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
        <Toaster position="top-right" />
      </AuthProvider>
    </BrowserRouter>
  );
}
