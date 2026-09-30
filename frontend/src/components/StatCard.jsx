import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";

function StatCard({ title, available, value, formatter }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent className="text-2xl font-bold">
        {available ? formatter(value) : "Data unavailable"}
      </CardContent>
    </Card>
  );
}

export default StatCard;