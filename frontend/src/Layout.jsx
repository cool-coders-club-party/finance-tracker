/*
==========================================
Layout renders the chosen page via Outlet
and adds Navbar to it. No longer have to
import Navbar to every page separately.
==========================================
*/

import { Outlet } from "react-router-dom";
import { Navbar } from "./components/Navbar";

function Layout() {
    return (
        <>
        <Navbar />
        <Outlet />
        </>
    );
}

export default Layout;