import {
  collection,
  addDoc,
  getDocs,
  doc,
  query,
  where,
  onSnapshot,
  serverTimestamp,
  updateDoc,
  arrayUnion,
} from "firebase/firestore";
import { db } from "./firebase";

export interface AcademicGroup {
  id: string;
  name: string; // e.g. "Software Engineering"
  code: string; // e.g. "SE101"
  joinCode: string; // Unique 6-char code for students to join
  lecturerName: string;
  repId: string; // The creator/admin
  memberCount: number;
  createdAt: number;
}

export interface GroupPost {
  id: string;
  groupId: string;
  authorId: string;
  authorName: string; // Cache name to reduce reads
  content: string;
  type: "announcement" | "general";
  createdAt: any; // Timestamp
}

// Helper to generate random 6-char join code
const generateJoinCode = () => {
  return Math.random().toString(36).substring(2, 8).toUpperCase();
};

export const createGroup = async (
  userId: string,
  data: { name: string; code: string; lecturerName: string }
) => {
  const joinCode = generateJoinCode();

  const groupData = {
    ...data,
    repId: userId,
    joinCode,
    memberCount: 1, // Start with creator
    members: [userId], // Simple array for small groups, or subcollection for huge ones. Array is cheaper/faster for <1000.
    createdAt: Date.now(),
  };

  const docRef = await addDoc(collection(db, "groups"), groupData);
  return { id: docRef.id, ...groupData };
};

export const joinGroup = async (userId: string, joinCode: string) => {
  // 1. Find the group by join code
  const q = query(collection(db, "groups"), where("joinCode", "==", joinCode));
  const snapshot = await getDocs(q);

  if (snapshot.empty) {
    throw new Error("Invalid join code");
  }

  const groupDoc = snapshot.docs[0];
  const groupData = groupDoc.data();
  const groupId = groupDoc.id;

  // 2. Check if already a member
  if (groupData.members && groupData.members.includes(userId)) {
    throw new Error("You are already a member of this group");
  }

  // 3. Add to members array and increment count
  await updateDoc(doc(db, "groups", groupId), {
    members: arrayUnion(userId),
    memberCount: (groupData.memberCount || 0) + 1,
  });

  return groupId;
};

// Subscribe to groups where the user is a member
export const subscribeToUserGroups = (
  userId: string,
  callback: (groups: AcademicGroup[]) => void
) => {
  if (!userId) return () => {};

  const q = query(
    collection(db, "groups"),
    where("members", "array-contains", userId)
  );

  return onSnapshot(
    q,
    (snapshot) => {
      const groups = snapshot.docs.map(
        (doc) =>
          ({
            id: doc.id,
            ...doc.data(),
          } as AcademicGroup)
      );

      callback(groups);
    },
    (error) => {
      console.error("Error subscribing to user groups:", error);
      // Don't crash the app, just log it. The UI will show empty state.
    }
  );
};

export const createPost = async (
  groupId: string,
  userId: string,
  authorName: string,
  content: string,
  type: "announcement" | "general" = "general"
) => {
  const postsRef = collection(db, "groups", groupId, "posts");
  await addDoc(postsRef, {
    groupId,
    authorId: userId,
    authorName,
    content,
    type,
    createdAt: serverTimestamp(),
  });
};

export const subscribeToPosts = (
  groupId: string,
  callback: (posts: GroupPost[]) => void
) => {
  const q = query(collection(db, "groups", groupId, "posts")); // Add orderBy logic in index later

  return onSnapshot(
    q,
    (snapshot) => {
      const posts = snapshot.docs.map(
        (doc) =>
          ({
            id: doc.id,
            ...doc.data(),
          } as GroupPost)
      );
      // Client side sort for now
      posts.sort(
        (a, b) =>
          (b.createdAt?.toMillis?.() || 0) - (a.createdAt?.toMillis?.() || 0)
      );
      callback(posts);
    },
    (error) => {
      console.error("Error subscribing to posts:", error);
    }
  );
};
