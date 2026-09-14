/*
========================================
Router setup for three placeholder pages
========================================
*/

import { createBrowserRouter, RouterProvider } from "react-router-dom";
import Home from "./pages/Home";
import Dashboard from "./pages/Dashboard";
import Transactions from "./pages/Transactions";

const router = createBrowserRouter([
    { path: "/", element: <Home /> },
    { path: "/dashboard", element: <Dashboard /> },
    { path: "/transactions", element: <Transactions /> },
]),

export default router;