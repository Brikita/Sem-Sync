import { GoogleGenerativeAI, SchemaType } from "@google/generative-ai";

const API_KEY = import.meta.env.VITE_GEMINI_API_KEY;
// Note: In production, move this to a backend to secure your key.

const genAI = new GoogleGenerativeAI(API_KEY || "");

export interface TimetableEntry {
  day: string;
  startTime: string;
  endTime: string;
  subject: string;
  room: string;
  type: "lecture" | "tutorial" | "lab" | "other";
  lecturer: string;
}

const timetableSchema = {
  description: "List of classes from a timetable",
  type: SchemaType.ARRAY,
  items: {
    type: SchemaType.OBJECT,
    properties: {
      day: { type: SchemaType.STRING },
      startTime: { type: SchemaType.STRING },
      endTime: { type: SchemaType.STRING },
      subject: { type: SchemaType.STRING },
      room: { type: SchemaType.STRING },
      type: {
        type: SchemaType.STRING,
        enum: ["lecture", "tutorial", "lab", "other"],
      },
      lecturer: { type: SchemaType.STRING },
    },
    required: ["day", "startTime", "endTime", "subject"],
  },
};

// Helper: Pause execution for X milliseconds
const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export async function extractTimetableFromImage(
  file: File,
): Promise<TimetableEntry[]> {
  if (!API_KEY) throw new Error("Gemini API key is not configured.");

  // We only use the one model that we know exists for your key
  const modelName = "gemini-2.0-flash-exp";
  const base64Data = await fileToGenerativePart(file);
  const prompt = "Analyze this timetable image and extract the schedule.";

  // Try up to 3 times with exponential backoff
  const maxRetries = 3;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      console.log(`Attempt ${attempt}: Using ${modelName}`);

      const model = genAI.getGenerativeModel({
        model: modelName,
        generationConfig: {
          responseMimeType: "application/json",
          responseSchema: timetableSchema,
        },
      });

      const result = await model.generateContent([prompt, base64Data]);
      const response = await result.response;
      return JSON.parse(response.text()) as TimetableEntry[];
    } catch (error: any) {
      console.warn(`Attempt ${attempt} failed:`, error.message);

      // Check if it's the "Too Many Requests" error
      if (error.message.includes("429")) {
        if (attempt === maxRetries) throw error; // Give up on last try

        // The error said "retry in 25s", so we wait 30s to be safe.
        console.log("Rate limit hit. Waiting 30 seconds before retry...");
        await wait(30000);
        continue;
      }

      // If it's a 404 or other error, don't retry, just throw
      throw error;
    }
  }

  throw new Error("Failed to extract timetable after multiple attempts.");
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
