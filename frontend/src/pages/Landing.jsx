import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
//link changes pages without reloading the app
import { Link } from "react-router-dom";

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