/*
========================================
Router setup for three placeholder pages
========================================
*/

import { createBrowserRouter } from "react-router-dom";
import Landing from "./pages/Landing";
import Dashboard from "./pages/Dashboard";
import Transactions from "./pages/Transactions";

const router = createBrowserRouter([
    { path: "/", element: <Dashboard /> },
//     { path: "/home", element: <Home /> },
//      { path: "/dashboard", element: <Dashboard /> },
    { path: "/transactions", element: <Transactions /> },
    { path: "/transactions/:transactionId/edit", element: <Transactions /> },
]);

export default router;