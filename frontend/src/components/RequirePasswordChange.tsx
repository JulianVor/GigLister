import { getSession } from "@/lib/session";
import { ChangePasswordForm } from "@/components/ChangePasswordForm";

/** Replaces the whole page with a forced "Passwort ändern" screen whenever the logged-in
 * account still carries a temporary password (see AdminService.createUser) - wraps every
 * page's `{children}` in the root layout, so there's no single route to redirect to/from
 * and no risk of a redirect loop. The backend enforces the same rule at the API level too
 * (see MustChangePasswordFilter) - this is just what makes it visible instead of every
 * click silently 403ing. Header/nav stay outside this, so "Abmelden" is always reachable. */
export async function RequirePasswordChange({ children }: { children: React.ReactNode }) {
  const session = await getSession();
  if (!session?.mustChangePassword) {
    return <>{children}</>;
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="font-display text-2xl">Passwort ändern</h1>
      <p className="mt-1 font-meta text-sm text-muted">
        Bevor es weitergeht: Setze ein eigenes Passwort. Nutze als &bdquo;aktuelles Passwort&ldquo; das temporäre
        Passwort, das du bekommen hast.
      </p>
      <ChangePasswordForm />
    </div>
  );
}
