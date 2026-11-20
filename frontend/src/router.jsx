import { createBrowserRouter, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import RequireAuth from "./components/RequireAuth";
import Dashboard from "./pages/Dashboard";
import Transactions from "./pages/Transactions";
import TransactionHistory from "./pages/TransactionHistory";
import Portfolio from "./pages/Portfolio";
import SignIn from "./pages/SignIn";
import Register from "./pages/Register";
import LogoutButton from "./components/LogoutButton";
import ProtectedRoute from "./components/ProtectedRoute";

const router = createBrowserRouter([
    { path: '/login', element: <Login key="login" /> },
    { path: '/register', element: <Login key="register" registration /> },
    // { path: '/new-login', element: <SignIn /> },
    // { path: '/new-register', element: <Register /> },
    // { path: '/new-logout', element: <LogoutButton /> },
    // { element: <ProtectedRoute />, children: [
    // { path: '/new-secret', element: <p className="p-8">Secret page. You are signed in.</p> },
    // ] },
    { element: <RequireAuth />, children: [
        { path: '/', element: <Dashboard /> },
        { path: '/transactions', element: <TransactionHistory /> },
        { path: '/transactions/new', element: <Transactions /> },
        { path: '/transactions/:transactionId/edit', element: <Transactions /> },
        { path: '/portfolio', element: <Portfolio /> },
        // Keep existing market-data links working after lookup moved into Portfolio.
        { path: '/market-data', element: <Navigate to="/portfolio" replace /> },
    ] },
]);

export default router;
