/*
========================================
Router setup for three placeholder pages
========================================
*/

import { createBrowserRouter, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import RequireAuth from "./components/RequireAuth";
import Dashboard from "./pages/Dashboard";
import Transactions from "./pages/Transactions";
import TransactionHistory from "./pages/TransactionHistory";
import Portfolio from "./pages/Portfolio";

const router = createBrowserRouter([
    { path: '/login', element: <Login key="login" /> },
    { path: '/register', element: <Login key="register" registration /> },
//     { element: <RequireAuth />, children: [
        { path: '/', element: <Dashboard /> },
        { path: '/transactions', element: <TransactionHistory /> },
        { path: '/transactions/new', element: <Transactions /> },
        { path: '/transactions/:transactionId/edit', element: <Transactions /> },
//    ] },
    { path: '/portfolio', element: <Portfolio /> },
    { path: '/market-data', element: <Navigate to="/portfolio" /> },
]);

export default router;
