/*
========================================
Router setup for three placeholder pages
========================================
*/

import { createBrowserRouter, Navigate } from "react-router-dom";
import Landing from "./pages/Landing";
import Dashboard from "./pages/Dashboard";
import Transactions from "./pages/Transactions";
import Portfolio from "./pages/Portfolio";

const router = createBrowserRouter([
    { path: "/", element: <Dashboard /> },
//     { path: "/home", element: <Home /> },
//      { path: "/dashboard", element: <Dashboard /> },
    { path: "/transactions", element: <Transactions /> },
    { path: "/transactions/:transactionId/edit", element: <Transactions /> },
    { path: "/portfolio", element: <Portfolio /> },
    { path: "/market-data", element: <Navigate to="/portfolio" /> }
]);

export default router;