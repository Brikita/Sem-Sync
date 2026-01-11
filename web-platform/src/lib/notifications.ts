import {
  collection,
  query,
  where,
  orderBy,
  onSnapshot,
  addDoc,
  updateDoc,
  doc,
  serverTimestamp,
  Timestamp,
  writeBatch,
} from "firebase/firestore";
import { db } from "./firebase";

export type NotificationType =
  | "announcement"
  | "deadline"
  | "class_update"
  | "assessment"
  | "system";

export interface AppNotification {
  id: string;
  userId: string;
  type: NotificationType;
  title: string;
  message: string;
  link?: string; // Optional deep link
  read: boolean;
  createdAt: Timestamp;
  metadata?: {
    groupId?: string;
    unitId?: string;
    postId?: string;
  };
}

/**
 * Subscribe to notifications for a user
 */
export const subscribeToNotifications = (
  userId: string,
  callback: (notifications: AppNotification[]) => void
) => {
  const notificationsRef = collection(db, "notifications");
  const q = query(
    notificationsRef,
    where("userId", "==", userId),
    orderBy("createdAt", "desc")
  );

  return onSnapshot(q, (snapshot) => {
    const notifications = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...doc.data(),
    })) as AppNotification[];
    callback(notifications);
  });
};

/**
 * Create a new notification
 */
export const createNotification = async (
  userId: string,
  type: NotificationType,
  title: string,
  message: string,
  options?: {
    link?: string;
    metadata?: AppNotification["metadata"];
  }
): Promise<string> => {
  const notificationsRef = collection(db, "notifications");
  const docRef = await addDoc(notificationsRef, {
    userId,
    type,
    title,
    message,
    link: options?.link || null,
    read: false,
    createdAt: serverTimestamp(),
    metadata: options?.metadata || null,
  });
  return docRef.id;
};

/**
 * Mark a single notification as read
 */
export const markNotificationAsRead = async (
  notificationId: string
): Promise<void> => {
  const notificationRef = doc(db, "notifications", notificationId);
  await updateDoc(notificationRef, { read: true });
};

/**
 * Mark all notifications as read for a user
 */
export const markAllNotificationsAsRead = async (
  _userId: string,
  notificationIds: string[]
): Promise<void> => {
  const batch = writeBatch(db);
  notificationIds.forEach((id) => {
    const notificationRef = doc(db, "notifications", id);
    batch.update(notificationRef, { read: true });
  });
  await batch.commit();
};

/**
 * Get unread count for a user
 */
export const getUnreadCount = (notifications: AppNotification[]): number => {
  return notifications.filter((n) => !n.read).length;
};
