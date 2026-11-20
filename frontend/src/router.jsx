import { createBrowserRouter, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import RequireAuth from "./components/RequireAuth";
import Dashboard from "./pages/Dashboard";
import Transactions from "./pages/Transactions";
import TransactionHistory from "./pages/TransactionHistory";
import Portfolio from "./pages/Portfolio";
// import SignIn from "./pages/SignIn";
// import Register from "./pages/Register";

const router = createBrowserRouter([
    { path: '/login', element: <Login key="login" /> },
    { path: '/register', element: <Login key="register" registration /> },
    // { path: '/new-login', element: <SignIn /> },
    // { path: '/new-register', element: <Register /> },
    { element: <RequireAuth />, children: [
        { path: '/', element: <Dashboard /> },
        { path: '/transactions', element: <TransactionHistory /> },
        { path: '/transactions/new', element: <Transactions /> },
        { path: '/transactions/:transactionId/edit', element: <Transactions /> },
        { path: '/portfolio', element: <Portfolio /> },
        { path: '/market-data', element: <Navigate to="/portfolio" replace /> },
    ] },
]);

export default router;
