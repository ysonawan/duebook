export interface UserProfile {
  id: number;
  name: string;
  email?: string;
  phone: string;
  secondaryEmails: string[];
}
