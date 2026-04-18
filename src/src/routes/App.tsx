import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import type { ReactElement } from "react";
import { Shell } from "@/components/Shell";
import { useSession, homeForUser } from "@/state/session";
import { LoginPage } from "@/pages/LoginPage";
import { RegisterPage } from "@/pages/RegisterPage";
import { HomePage } from "@/pages/HomePage";
import { MoodPage } from "@/pages/MoodPage";
import { ChatPage } from "@/pages/ChatPage";
import { AssessmentPage } from "@/pages/AssessmentPage";
import { CounselorsPage } from "@/pages/CounselorsPage";
import { BookingPage } from "@/pages/BookingPage";
import { AppointmentsPage } from "@/pages/AppointmentsPage";
import { ReportPage } from "@/pages/ReportPage";
import { MeditationPage } from "@/pages/MeditationPage";
import { ProfilePage } from "@/pages/ProfilePage";
import { CounselorDashboardPage } from "@/pages/CounselorDashboardPage";
import { AuditPendingPage } from "@/pages/AuditPendingPage";
import { AdminDashboardPage } from "@/pages/AdminDashboardPage";

function Guard({ role, children }: { role?: "user" | "counselor" | "admin"; children: ReactElement }) {
  const session = useSession();
  const location = useLocation();
  if (!session.isLoggedIn) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (!session.user) return children;
  if (session.user.role.toUpperCase() === "COUNSELOR" && session.user.status === 2 && location.pathname !== "/counselor/audit-pending") {
    return <Navigate to="/counselor/audit-pending" replace />;
  }
  if (role && session.user.role.toLowerCase() !== role) {
    return <Navigate to={homeForUser(session.user.role, session.user.status)} replace />;
  }
  return children;
}

function PublicOnly({ children }: { children: ReactElement }) {
  const session = useSession();
  if (session.isLoggedIn && session.user) return <Navigate to={homeForUser(session.user.role, session.user.status)} replace />;
  return children;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<PublicOnly><LoginPage /></PublicOnly>} />
      <Route path="/register" element={<PublicOnly><RegisterPage /></PublicOnly>} />
      <Route path="/" element={<Guard><Shell /></Guard>}>
        <Route index element={<Navigate to="/home" replace />} />
        <Route path="home" element={<Guard role="user"><HomePage /></Guard>} />
        <Route path="mood-diary" element={<Guard role="user"><MoodPage /></Guard>} />
        <Route path="mood-diary/:id" element={<Guard role="user"><MoodPage /></Guard>} />
        <Route path="ai-chat" element={<Guard role="user"><ChatPage /></Guard>} />
        <Route path="assessment" element={<Guard role="user"><AssessmentPage /></Guard>} />
        <Route path="assessment/:scaleKey" element={<Guard role="user"><AssessmentPage /></Guard>} />
        <Route path="assessment/result/:recordId" element={<Guard role="user"><AssessmentPage /></Guard>} />
        <Route path="assessment/history" element={<Guard role="user"><AssessmentPage /></Guard>} />
        <Route path="counselor-list" element={<Guard role="user"><CounselorsPage /></Guard>} />
        <Route path="booking/:counselorId" element={<Guard role="user"><BookingPage /></Guard>} />
        <Route path="my-appointments" element={<Guard role="user"><AppointmentsPage /></Guard>} />
        <Route path="emotion-report" element={<Guard role="user"><ReportPage /></Guard>} />
        <Route path="meditation" element={<Guard role="user"><MeditationPage /></Guard>} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="counselor/dashboard" element={<Guard role="counselor"><CounselorDashboardPage /></Guard>} />
        <Route path="counselor/audit-pending" element={<Guard role="counselor"><AuditPendingPage /></Guard>} />
        <Route path="admin/dashboard" element={<Guard role="admin"><AdminDashboardPage /></Guard>} />
      </Route>
    </Routes>
  );
}
