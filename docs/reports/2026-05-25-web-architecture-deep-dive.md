# Web Architecture Deep Dive - EduLife

## Architecture Overview

EduLife web version uses modern, edge-first architecture optimized for performance and developer experience.

### Key Architectural Decisions

**1. Edge-First Deployment (Cloudflare Workers)**
- Deploy application code globally to 200+ edge locations
- Zero cold starts, instant response times
- Single runtime for both SSR and client hydration
- Cost-efficient pay-per-request model

**2. Full-Stack TypeScript**
- Single language across server and client code
- Type safety from API contract to UI rendering
- Shared validation schemas (Zod)
- Better developer experience with IDE support

**3. React + TanStack Ecosystem**
- React 19 for reactive UI components
- TanStack Start for SSR and meta-framework features
- TanStack Router for file-based routing with type-safety
- TanStack Query for data fetching and caching

**4. Component-Driven UI (Radix UI + Tailwind)**
- Unstyled, accessible components from Radix UI
- Utility-first styling with Tailwind CSS
- Design system consistency without duplication
- Rapid prototyping and maintenance

## Web Architecture Layers

### Client Layer
```
Browser (Chrome, Firefox, Safari, Edge)
↓
HTML/CSS/JavaScript (initially from SSR)
↓
React Hydration
```

### Server Layer (Edge)
```
Cloudflare Workers Runtime
↓
TanStack Start (SSR handler)
↓
Express-like routing
↓
Stream HTML to browser
```

### Component Layer
```
TanStack Router (file-based routing)
└── Pages (index.tsx, login.tsx, dashboard.tsx)
    └── React Components (Radix UI + Tailwind)
        ├── Forms (HookForm + Zod validation)
        ├── Data Display (Recharts, Tables)
        ├── Navigation (Breadcrumb, Tabs)
        └── Modals & Dialogs
```

### State Management Layer

**Data Fetching & Caching (TanStack Query)**
```javascript
const { data, isPending, error } = useQuery({
  queryKey: ['courses'],
  queryFn: () => fetch('/api/v1/courses')
});
```
- Automatic caching of API responses
- Deduplication of requests
- Automatic refetch on focus/reconnect
- Stale-while-revalidate pattern

**Local UI State (React Hooks)**
```javascript
const [isOpen, setIsOpen] = useState(false);
const [formData, setFormData] = useState({});
```
- Modal open/close state
- Form field values
- Filter selections
- Sort order

### API Communication Layer

**Firebase Authentication**
- Browser native Firebase SDK
- Login/Register with email/password or OAuth
- Automatic token refresh
- Secure session management

**HTTP Client (Fetch API)**
- TanStack Query handles actual HTTP requests
- Automatic Bearer token injection from Firebase
- Retry logic for failed requests
- Error boundary handling

**API Contract**
```
POST /api/v1/auth/sync
GET /api/v1/courses
GET /api/v1/courses/{courseId}
POST /api/v1/enrollments
GET /api/v1/enrollments
```

### Backend Integration

- Shared API contracts with Android
- Same Firebase Auth validation
- Same database schema
- Real-time data consistency

## Data Flow

### User Login Flow
1. User visits web app
2. Cloudflare Worker serves SSR'd HTML
3. React hydrates in browser
4. User clicks "Login"
5. Firebase SDK opens auth modal
6. After successful auth, Firebase returns ID token
7. Token stored in browser (Firebase SDK handles)
8. Subsequent API calls include Bearer token
9. Backend validates token with Firebase Admin SDK
10. User data synced with /api/v1/auth/sync
11. User state stored in TanStack Query cache
12. UI renders authenticated dashboard

### Course Browsing Flow
1. User navigates to /explore
2. TanStack Router loads Explore page component
3. useQuery hook fetches GET /api/v1/courses
4. Query suspends rendering (loading state)
5. API response cached in Query storage
6. React renders course list with Radix UI cards
7. User clicks course
8. Router navigates to /courses/{courseId}
9. useQuery for GET /api/v1/courses/{courseId}
10. Course detail renders with sections/lessons
11. Back to courses: Query cache hit (instant)

### Enrollment Flow
1. User on course detail clicks "Enroll"
2. useMutation triggered: POST /api/v1/enrollments
3. Optimistic update in UI (button disabled)
4. API response received
5. Query cache invalidated for /enrollments
6. Dashboard re-fetches enrollments list
7. User sees new enrollment immediately

## Performance Optimizations

**SSR (Server-Side Rendering)**
- HTML generated on edge, streamed to browser
- Initial page load doesn't require JavaScript
- SEO-friendly (no crawl-time rendering)
- Faster first contentful paint (FCP)

**Code Splitting & Lazy Loading**
- TanStack Router lazy-loads page components
- Route chunks only download when navigated
- Reduces initial bundle size

**Caching Strategy**
- TanStack Query caches all API responses
- Stale-while-revalidate: serve stale data while refetching
- Automatic refetch when:
  - Window regains focus
  - Connection restored
  - Mutation invalidates related queries
  - Explicit refetch triggered

**Compression & Assets**
- Vite bundles and minifies code
- CSS inlined in HTML for critical paths
- Remaining CSS lazy-loaded
- Images optimized via URL parameters

## Comparison: Web vs Android

| Aspect | Web | Android |
|--------|-----|---------|
| **Client Runtime** | Browser JS Engine | Dalvik/ART VM |
| **UI Framework** | React + Radix UI | XML Layouts + Jetpack Compose |
| **State Management** | TanStack Query | ViewModel + LiveData |
| **HTTP Client** | Fetch API | OkHttp + Retrofit |
| **Form Validation** | Zod + HookForm | Manual or DataBinding |
| **Routing** | TanStack Router | Navigation Component |
| **Deployment** | Cloudflare Workers | Google Play Store |
| **Backend** | ✅ Same `/api/v1/*` | ✅ Same `/api/v1/*` |
| **Auth** | ✅ Firebase | ✅ Firebase |
| **Database** | ✅ PostgreSQL | ✅ PostgreSQL |

## Diagrams

### 1. Web Architecture Diagram
Shows internal web application structure:
- Browser and rendering
- TanStack Start SSR
- React Components (Radix UI)
- TanStack Query state management
- Firebase Auth
- Backend API endpoints
- PostgreSQL database

**Use case:** Understanding web app internals

### 2. Unified Platform Architecture Diagram
Shows web and mobile both connecting to same backend:
- Android app (Kotlin/MVVM)
- Web app (React/TanStack)
- Shared Firebase Auth
- Shared Spring Boot API
- Shared PostgreSQL database
- Security layer & RBAC

**Use case:** Understanding multi-platform strategy, code reuse

### 3. Web & Mobile Data Synchronization
Shows how same user sees same data on both platforms:
- User logs in on web, then mobile
- Enrolls in course on web
- Sees enrollment on mobile immediately
- Progress updates sync across platforms

**Use case:** Understanding data consistency guarantee

### 4. Web Tech Stack & Components
Detailed view of all technologies:
- Browser runtime
- Cloudflare Workers edge
- React + TanStack stack
- Component architecture (Pages, UI, Forms)
- State management (Query, Hooks)
- Data & Animation layers
- API communication
- Build & deployment tools (Vite, Wrangler)

**Use case:** Tech decisions, vendor lock-in, migration path

## Future Enhancements

**Planned**
- Service Workers for offline mode
- Progressive Web App (PWA) manifest
- Push notifications for course updates
- Real-time updates (WebSocket or Server-Sent Events)

**Possible**
- Micro frontends for scalability
- GraphQL layer for optimized queries
- Edge-based caching rules
- A/B testing framework

## Advantages of This Architecture

✅ **Zero server management** - Cloudflare handles infrastructure
✅ **Global scalability** - Edge deployment in 200+ locations
✅ **Cost efficient** - Pay only for actual usage
✅ **Type-safe** - Full-stack TypeScript
✅ **Developer experience** - HMR dev server, fast builds
✅ **Performance** - SSR, code splitting, caching
✅ **Maintenance** - Single codebase, shared backend
✅ **SEO** - Server-side rendering out of box
✅ **Accessibility** - Radix UI ensures WCAG compliance

## Constraints & Trade-offs

⚠️ **Cloudflare Workers limits**
- 30s execution timeout (covers most use cases)
- No persistent connections (stateless)
- Limited file system access

⚠️ **Edge computing model**
- Cold regions have slight latency spike
- Database queries still hit centralized backend
- Not ideal for compute-heavy operations

⚠️ **React size**
- Minimum bundle ~35KB gzipped
- SSR reduces impact but still trade-off

Solutions: Migrate heavy compute to backend, use regional caching, lazy-load routes
