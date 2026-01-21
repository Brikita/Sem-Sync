import { createClient } from "@supabase/supabase-js";

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;

if (
  !supabaseUrl ||
  !supabaseAnonKey ||
  supabaseUrl.includes("your_supabase_url")
) {
  console.error(
    "⚠️ Supabase Config Error: Environment variables are missing or default.",
  );
  console.error(
    "Please update .env.local with your actual Supabase URL and Key.",
  );
}

export const supabase = createClient(
  supabaseUrl || "https://xyzcompany.supabase.co",
  supabaseAnonKey || "public-anon-key",
);
