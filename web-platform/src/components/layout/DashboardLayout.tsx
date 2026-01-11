import { useState } from "react";
import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";
import TopNav from "./TopNav";
import OfflineBanner from "./OfflineBanner";

export default function DashboardLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <>
      {/* Offline Banner */}
      <OfflineBanner />

      <div className="flex min-h-screen bg-background text-foreground">
        {/* Sidebar - Hidden on mobile by default */}
        <Sidebar open={sidebarOpen} setOpen={setSidebarOpen} />

        {/* Main Content Area */}
        <div className="flex flex-1 flex-col transition-all duration-300 ease-in-out">
          <TopNav onMenuClick={() => setSidebarOpen(true)} />

          <main className="flex-1 p-4 md:p-6 lg:p-8">
            <Outlet />
          </main>
        </div>
      </div>
    </>
  );
}
