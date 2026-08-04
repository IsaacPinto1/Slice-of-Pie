# Deploying Slice of Pie

Quick note on the split, since it isn't a single "deploy button" setup:

- **Vercel** hosts the frontend (static Vite build).
- **Supabase** hosts the Postgres database only - we don't use Supabase Auth,
  since the backend already has its own JWT-based login.
- **Render** hosts the Spring Boot backend. Vercel can't run a long-lived
  JVM process, so the API needs a separate host. Render is used here because
  it has a simple free tier and builds straight from the included
  `backend/Dockerfile`; swap in Railway/Fly/etc if you'd rather.

Set up the database first, then the backend (it needs to be reachable before
the frontend can call it), then the frontend last.

## 1. Supabase (database)

1. Create a project at https://supabase.com/dashboard.
2. Go to **Project Settings -> Database -> Connection string**, and switch
   to the **Session pooler** tab (not "Direct connection" - the pooler is
   IPv4-reachable, which matters since most PaaS hosts don't support IPv6-only
   direct connections).
3. Note down the host, the username (looks like `postgres.<project-ref>`),
   and the database password you set when creating the project.
4. You do **not** need to run any SQL by hand - Flyway will create all the
   tables automatically on the backend's first boot, same as it does locally.

## 2. Render (backend)

1. Push this repo to GitHub if it isn't already (Render deploys from a repo).
2. In Render, **New -> Web Service**, connect the repo, and set:
   - **Root Directory**: `backend`
   - **Runtime**: Docker (Render will pick up `backend/Dockerfile`
     automatically)
3. Add these environment variables in the Render dashboard (**not** in a
   `.env` file - that file is only for local runs):

   | Key | Value |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<supabase-pooler-host>:5432/postgres` |
   | `SPRING_DATASOURCE_USERNAME` | `postgres.<your-project-ref>` |
   | `SPRING_DATASOURCE_PASSWORD` | your Supabase DB password |
   | `JWT_SECRET` | a long random string (`openssl rand -base64 48` works) |
   | `FINNHUB_API_KEY` | your Finnhub key |
   | `CORS_ALLOWED_ORIGINS` | your Vercel URL - you can fill this in after step 3, then redeploy |

   `SPRING_PROFILES_ACTIVE=prod` is already baked into the Dockerfile, so you
   don't need to set it again here.
4. Deploy. Once it's up, Render gives you a URL like
   `https://sliceofpie-api.onrender.com` - note it down, the frontend needs it.
5. Sanity check: `curl https://<your-render-url>/auth/register -X POST -H "Content-Type: application/json" -d '{"username":"test","password":"test1234"}'`
   should return `201 Created`, not a connection or 500 error.

## 3. Vercel (frontend)

1. In Vercel, **Add New -> Project**, import the repo, and set:
   - **Root Directory**: `frontend`
   - Framework preset should auto-detect as Vite.
2. Add an environment variable:
   - `VITE_API_URL` = your Render URL from step 2 (e.g.
     `https://sliceofpie-api.onrender.com`)
3. Deploy. Vercel gives you a URL like `https://slice-of-pie.vercel.app`.
4. Go back to Render and set `CORS_ALLOWED_ORIGINS` to that Vercel URL
   (comma-separate multiple values if you also want preview deployment URLs
   allowed), then redeploy the backend so it picks up the change.

## Notes

- Vite only inlines `VITE_`-prefixed env vars at **build time**, so changing
  `VITE_API_URL` in Vercel requires a redeploy to take effect - it's not read
  at runtime.
- Render's free tier spins down after inactivity, so the first request after
  idle time can take ~30-60s to wake back up. That's expected, not a bug.
- If you rotate `JWT_SECRET` in prod, every existing logged-in user gets
  booted out (their token stops validating) - the frontend's 401 handler
  will bounce them to `/login` automatically.
