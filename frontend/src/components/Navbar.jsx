/*
=======================
Simple Navbar component
=======================
*/

import { Link } from "react-router-dom";

function Navbar() {
    return (
        <nav>
            <Link to="/">Home</Link>
            <Link to="/dashboard">Dashboard</Link>
            <Link to="/transactions">Transactions</Link>
        </nav>
        );
    }

export default Navbar;