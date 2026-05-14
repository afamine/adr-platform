export interface NotificationApiDto {
  id: string;
  type: string;
  title: string;
  body: string;
  adrId: string | null;
  isRead: boolean;
  createdAt: string;
  timeAgo: string;
}

export interface BellNotification {
  id: string;
  type: string;
  dotColor: string;
  title: string;
  body: string;
  time: string;
  action: string;
  adrId: string | null;
  unread: boolean;
}
