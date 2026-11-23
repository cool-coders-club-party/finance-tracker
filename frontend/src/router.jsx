import { createBrowserRouter, Navigate } from "react-router-dom";
import Dashboard from "./pages/Dashboard";
import Transactions from "./pages/Transactions";
import TransactionHistory from "./pages/TransactionHistory";
import Portfolio from "./pages/Portfolio";
import SignIn from "./pages/SignIn";
import Register from "./pages/Register";
import ProtectedRoute from "./components/ProtectedRoute";


const router = createBrowserRouter([
    { path: '/login', element: <SignIn key="login" /> },
    { path: '/register', element: <Register key="register" registration /> },
    { element: <ProtectedRoute />, children: [
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
