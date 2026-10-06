import { Card, CardHeader, CardTitle, CardContent, CardAction, CardFooter } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ArrowUpRight, ArrowDownRight } from "lucide-react";

function StatCard({ title, icon: Icon, available, value, formatter, change, caption, positiveIsGood }) {
  const isUp = change >= 0;
  const isFavourable = isUp === positiveIsGood;
  const TrendIcon = isUp ? ArrowUpRight : ArrowDownRight;

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        <CardAction> {Icon && <Icon className="size-4 text-mutated-foreground" />} </CardAction>
      </CardHeader>
      <CardContent className="text-2xl font-bold">
          {available ? formatter(value) : "Data Unavailable"}
      </CardContent>
      {available && (
          <CardFooter className="gap-2 test-xs text-muted-foreground">
              <Badge variant={isFavourable ? "secondary" : "destructive"}>
                  <TrendIcon className="size-3" />
                  {Math.abs(change).toFixed(1)}%
              </Badge>
              <span>{caption}</span>
          </CardFooter>
      )}
    </Card>
  );
}

export default StatCard;