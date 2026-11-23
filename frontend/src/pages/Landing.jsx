import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Link } from "react-router-dom";

/**
 * Renders the legacy landing placeholder with client-side navigation.
 * This component is not registered in the current router.
 *
 * @return {import("react").ReactElement} The landing placeholder.
 */
function Landing() {
    return (
        <section>
            <div className="text-center">
                <h1>Temp Temp Temp</h1>
                <p>Temp Desc Temp Desc Temp Desc Temp Desc</p>
                <Button asChild>
                    <Link to="/dashboard">Get Started</Link>
                </Button>
            </div>
            <div>
                <CardHeader>oooo</CardHeader>
            </div>
        </section>
    );
}

export default Landing;