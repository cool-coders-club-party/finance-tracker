import DashboardShell from "../components/DashboardShell";

export default function MarketData() {
    return (
        <DashboardShell>
            <main className="space-y-6 text-left">
                <header className="space-y-2">
                    <h1 className="text-3xl font-bold">Market Data</h1>
                    <p className="text-sm text-muted-foreground">
                        Look up the latest available stock price.
                    </p>
                </header>

                {/* will add the ticker here. */}
                </main>
            </DashboardShell>

        );
    }