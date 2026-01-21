import { GoogleGenerativeAI } from "@google/generative-ai";

const API_KEY = import.meta.env.VITE_GEMINI_API_KEY;

if (!API_KEY || API_KEY.includes("your_gemini_api_key")) {
  console.warn(
    "⚠️ Gemini Config Error: API Key is missing or default. AI Timetable extraction will fail.",
  );
}

const genAI = new GoogleGenerativeAI(API_KEY || "dummy-key");

export interface TimetableEntry {
  day: string;
  startTime: string; // HH:mm format
  endTime: string; // HH:mm format
  subject: string;
  room?: string;
  type?: "lecture" | "tutorial" | "lab" | "other";
  lecturer?: string;
}

export async function extractTimetableFromImage(
  file: File,
): Promise<TimetableEntry[]> {
  if (!API_KEY) {
    throw new Error("Gemini API key is not configured.");
  }

  try {
    // Note: Using a specific available model. if 1.5-flash fails, try 'gemini-1.5-flash-latest'
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

    // Convert file to base64
    const base64Data = await fileToGenerativePart(file);

    const prompt = `
      Analyze this timetable image and extract the schedule into a strict JSON array.
      Identify the following for each class session:
      - day (Full English name: Monday, Tuesday, etc.)
      - startTime (24-hour format HH:mm)
      - endTime (24-hour format HH:mm)
      - subject (Course code AND Name if available, e.g. "CSC101 Introduction to Programming")
      - room (Location/Venue)
      - type (Lecture, Tutorial, Lab) - default to "lecture" if unsure.
      - lecturer (Name if available)

      If the image is not a timetable or is unreadable, return an empty array [].
      
      Return ONLY the raw JSON array. Valid JSON only. Do not wrap in markdown code blocks like \`\`\`json.
    `;

    const result = await model.generateContent([prompt, base64Data]);
    const response = await result.response;
    const text = response.text();

    console.log("Raw Geminii response:", text);

    // Clean up the response if it contains markdown formatting despite instructions
    const cleanedText = text
      .replace(/```json/g, "")
      .replace(/```/g, "")
      .trim();

    try {
      const data = JSON.parse(cleanedText);
      if (Array.isArray(data)) {
        return data as TimetableEntry[];
      }
      return [];
    } catch (e) {
      console.error("Failed to parse JSON from AI response", e);
      throw new Error("The AI response was not valid data.");
    }
  } catch (error) {
    console.error("Error extraction timetable:", error);
    throw new Error(
      "Failed to extract timetable data. Please ensure the image is clear and try again.",
    );
  }
}

async function fileToGenerativePart(file: File) {
  const base64EncodedDataPromise = new Promise((resolve) => {
    const reader = new FileReader();
    reader.onloadend = () => resolve((reader.result as string).split(",")[1]);
    reader.readAsDataURL(file);
  });

  return {
    inlineData: {
      data: (await base64EncodedDataPromise) as string,
      mimeType: file.type,
    },
  };
}
