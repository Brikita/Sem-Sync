export default function DashboardPage() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">
          Welcome back! Here's what's happening today.
        </p>
      </div>

      {/* Bento Grid Placeholder */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-7">
        <div className="col-span-4 rounded-xl border bg-card text-card-foreground shadow h-[300px] p-6">
          <h3 className="font-semibold">Today's Schedule</h3>
          {/* Calendar widget will go here */}
        </div>
        <div className="col-span-3 rounded-xl border bg-card text-card-foreground shadow h-[300px] p-6">
          <h3 className="font-semibold">Upcoming Tasks</h3>
          {/* Task list will go here */}
        </div>
        <div className="col-span-3 rounded-xl border bg-card text-card-foreground shadow h-[300px] p-6">
          <h3 className="font-semibold">Recent Notes</h3>
        </div>
        <div className="col-span-4 rounded-xl border bg-card text-card-foreground shadow h-[300px] p-6">
          <h3 className="font-semibold">Announcements</h3>
        </div>
      </div>
    </div>
  );
}
