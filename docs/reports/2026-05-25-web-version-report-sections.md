# Web Version Report Sections

These LaTeX sections should be added to `rapport PFA/untitled-1.tex` to document the web version.

## Section to add to Chapter "Méthodologie et avancement du projet" after "Avancement actuel"

```latex
\subsection{Version web parallèle}

En parallèle du développement mobile, une version web complète de la plateforme EduLife est en cours de construction. Cette version web utilise une architecture moderne et réutilise le même backend que l'application Android. Elle offre une expérience utilisateur optimisée pour les navigateurs de bureau et tablettes, tandis que l'application Android cible les smartphones.

La version web permet aux utilisateurs d'accéder à la plateforme via navigateur sans installation, élargissant l'audience et offrant une flexibilité d'accès supplémentaire. Les deux clients (Android et web) partagent les mêmes endpoints d'API, garantissant une cohérence métier et une synchronisation complète du state utilisateur (authentification, inscriptions, progression).
```

## New Chapter "Architecture web" to add after "Architecture du projet" chapter

```latex
\chapter{Architecture web}

\section{Vue d'ensemble}

La version web d'EduLife est construite avec un stack technologique moderne et performant, basé sur React et déployée sur Cloudflare Workers pour une scalabilité et une disponibilité globales. L'architecture web suit les mêmes principes que la version Android : réutiliser le backend unifié, valider rapidement les flux utilisateur, et maintenir une séparation claire entre le rendu, la logique métier et la communication réseau.

\section{Architecture web et organisation du code}

L'application web suit une architecture composant-centric avec une clear separation of concerns :

\begin{itemize}
    \item \textbf{Landing Page} : page d'accueil statique ou semi-statique presentant EduLife, ses caractéristiques, le problème résolu et un appel à l'action pour l'inscription ou la connexion.
    \item \textbf{Pages d'authentification} : formulaires de connexion et d'inscription, intégrés avec Firebase Auth comme sur Android.
    \item \textbf{Dashboard / Home} : tableau de bord principal montrant les cours auquel l'utilisateur est inscrit, la progression et un accès rapide au catalogue.
    \item \textbf{Explore / Course Catalog} : liste des cours disponibles avec filtrage et pagination, utilisant les mêmes endpoints \texttt{GET /api/v1/courses} que Android.
    \item \textbf{Course Detail} : affichage détaillé d'un cours avec ses sections et leçons, chargé via \texttt{GET /api/v1/courses/\{courseId\}}.
    \item \textbf{Lesson Player} : lecteur de leçon interactif pour la consommation de contenu vidéo, texte ou autre.
    \item \textbf{User Profile} : gestion du profil utilisateur, préférences et historique.
\end{itemize}

L'organisation du code source suit une structure claire :

\begin{verbatim}
guided-journey-lab/src/
  routes/              # TanStack Router pages
    index.tsx          # Landing page
    login.tsx
    register.tsx
    dashboard.tsx      # Home dashboard
    explore.tsx        # Course catalog
    courses.tsx        # Course detail
    level.tsx          # Lesson player
  components/
    landing/           # Landing page components
    ui/                # Reusable UI components (Radix UI)
  lib/                 # Utilities, helpers
  server.ts            # Server-side rendering setup
  router.tsx           # Router configuration
  styles.css           # Global styles (Tailwind)
\end{verbatim}

\section{Full-Stack JavaScript/TypeScript}

L'ensemble de la pile est écrit en TypeScript pour une sécurité de type et une maintenabilité accrues. Le code s'exécute à la fois côté client (navigateur) et côté serveur (Cloudflare Workers), ce qui permet un rendu côté serveur (SSR) pour une meilleure performance et un meilleur support SEO.

\section{Système de composants}

L'application web utilise une bibliothèque de composants moderne basée sur Radix UI et Tailwind CSS. Les composants sont réutilisables, accessibles et cohérents visuellement.

Composants principaux :

\begin{itemize}
    \item \textbf{Button, Input, Form} : formulaires et contrôles de base.
    \item \textbf{Card, Dialog, Tabs} : structures de contenu et modales.
    \item \textbf{Navigation, Sidebar, Breadcrumb} : navigation hiérarchique.
    \item \textbf{Avatar, Badge, Progress} : composants d'état et d'identité.
    \item \textbf{Accordion, Collapsible} : sections pliables pour hiérarchies pédagogiques.
    \item \textbf{Charts (Recharts)} : affichage de données de progression.
\end{itemize}

\section{Gestion d'état et appels API}

L'état applicatif et le cache des données réseau sont gérés par TanStack React Query. Celui-ci gère automatiquement :

\begin{itemize}
    \item la mise en cache des réponses API ;
    \item la réutilisation des données entre pages et composants ;
    \item le refetch automatique quand données stalent ;
    \item la gestion des états de chargement et d'erreur.
\end{itemize}

Les appels API utilisent les mêmes endpoints que l'application Android, avec les mêmes tokens Firebase et la même sécurité.

\section{Routing et navigation}

Le routage est géré par TanStack Router, qui offre :

\begin{itemize}
    \item navigation file-based pour une organisation scalable ;
    \item lazy loading des routes pour réduire la taille du bundle ;
    \item type-safe routing avec TypeScript ;
    \item état de routeur persisté quand approprié.
\end{itemize}

Les routes protégées (dashboard, profil, mes cours) requièrent une authentification Firebase valide.

\section{Déploiement web}

L'application web est déployée sur \textbf{Cloudflare Workers}, une plateforme de computing edge global. Cela offre :

\begin{itemize}
    \item temps de réponse proche du zéro pour les utilisateurs mondiaux ;
    \item pas de frais de serveur traditionnel (pay-per-request) ;
    \item support natif du SSR et du hybrid rendering ;
    \item intégration facile avec les services Cloudflare (KV, R2, etc.).
\end{itemize}
```

## New Section in "Technologies utilisées" chapter - add new section

```latex
\section{Web Frontend}

\techbox{edulife-logos/react.png}{React 19}
{React est la bibliothèque JavaScript utilisée pour construire l'interface utilisateur web.}
{Elle permet un rendu composant-centric réactif et efficace.}

\techbox{edulife-logos/typescript.png}{TypeScript}
{TypeScript fournit la sécurité des types pour la version web.}
{Elle s'exécute sur le client et sur le serveur pour un code unifié et maintenable.}

\techbox{edulife-logos/tanstack.png}{TanStack Start}
{TanStack Start est un meta-framework React pour le rendu côté serveur (SSR) et les routes.}
{Il simplifie le build, le SSR et l'optimisation des performances web.}

\techbox{edulife-logos/tanstack.png}{TanStack Router}
{TanStack Router gère le routage file-based et la navigation entre pages.}
{Type-safe routing offre une expérience développeur optimale.}

\techbox{edulife-logos/tanstack.png}{TanStack React Query}
{TanStack React Query gère l'état serveur et le cache des données réseau.}
{Il élimine le boilerplate et offre une synchronisation automatique.}

\techbox{edulife-logos/tailwind.png}{Tailwind CSS}
{Tailwind CSS est un framework CSS utility-first pour la stylisation rapide.}
{Elle garantit une cohérence visuelle et réduit le CSS custom.}

\techbox{edulife-logos/radix.png}{Radix UI}
{Radix UI fournit des composants accessibles et non-stylisés.}
{Ils s'intègrent parfaitement avec Tailwind CSS pour un design system cohérent.}

\techbox{edulife-logos/framer.png}{Framer Motion}
{Framer Motion permet les animations et les transitions fluides.}
{Elle offre une expérience utilisateur polished et moderne.}

\techbox{edulife-logos/reacthookform.png}{React Hook Form}
{React Hook Form gère la validation et la soumission des formulaires.}
{Elle réduit le rendu et offre une meilleure performance que les alternatives.}

\techbox{edulife-logos/zod.png}{Zod}
{Zod est une bibliothèque de validation TypeScript-first.}
{Elle valide les schémas de données avec type-safety complète.}

\techbox{edulife-logos/recharts.png}{Recharts}
{Recharts offre des graphiques composable pour afficher la progression et les données.}
{Elle s'intègre simplement avec React et Tailwind.}

\section{Web Deployment}

\techbox{edulife-logos/cloudflare.png}{Cloudflare Workers}
{Cloudflare Workers exécute le code JavaScript/TypeScript à la edge globale.}
{Elle offre une latence ultra-faible et une scalabilité illimitée.}

\techbox{edulife-logos/vite.png}{Vite}
{Vite est un bundler et dev server moderne ultra-rapide.}
{Elle accélère le développement et optimise le build de production.}
```

## New Section "Architecture backend unifiée"

Add this as a new section after the web architecture chapter:

```latex
\chapter{Architecture backend unifiée}

\section{Approche multi-client}

Le backend Spring Boot d'EduLife est conçu pour servir plusieurs clients simultanément : l'application Android mobile-native et la version web basée sur navigateur. Cette approche multi-client offre plusieurs avantages :

\begin{itemize}
    \item \textbf{Une seule source de vérité métier} : les règles d'authentification, d'autorisation et de logique métier sont définies une fois au backend et appliquées à tous les clients.
    \item \textbf{Synchronisation complète du state} : les données de l'utilisateur (inscriptions, progression, profil) sont centralisées et synchronisées en temps réel entre Android et le web.
    \item \textbf{Maintenabilité simplifiée} : les changements métier et de sécurité n'ont lieu qu'au backend, sans duplication de logique dans chaque client.
    \item \textbf{Scalabilité} : le backend est optimisé une seule fois pour supporter tous les clients, sans avoir à dupliquer les optimisations.
\end{itemize}

\section{Contrats API unifiés}

Tous les endpoints REST du backend utilisent le préfixe \texttt{/api/v1} et appliquent des contrats JSON cohérents pour tous les clients.

Les endpoints clés servis par le backend :

\begin{itemize}
    \item \texttt{POST /api/v1/auth/sync} : synchronisation utilisateur Firebase (utilisé par Android et web).
    \item \texttt{GET /api/v1/courses} : liste paginée des cours (découverte), accédée par les deux clients.
    \item \texttt{GET /api/v1/courses/\{courseId\}} : détail d'un cours avec sections et leçons.
    \item \texttt{POST /api/v1/enrollments} : inscription à un cours.
    \item \texttt{GET /api/v1/enrollments} : liste des inscriptions utilisateur.
    \item \texttt{DELETE /api/v1/enrollments/\{enrollmentId\}} : désinscription (futures sprints).
    \item \texttt{GET /api/v1/lessons/\{lessonId\}} : détail d'une leçon avec contenu (futures sprints).
    \item \texttt{POST /api/v1/progress} : sauvegarde de la progression (futures sprints).
\end{itemize}

\section{Sécurité et authentification unifiées}

Tous les clients (Android et web) utilisent Firebase Auth pour l'authentification initiale, puis envoient le token Firebase en header \texttt{Authorization: Bearer}.

Le backend valide ce token avec le Firebase Admin SDK, crée une authentification Spring Security et applique les mêmes contrôles d'accès et autorisations pour tous les clients.

Cette unification garantit que :

\begin{itemize}
    \item un utilisateur connecté sur Android voit les mêmes données s'il se connecte sur le web ;
    \item une action d'inscription sur le web se reflète immédiatement dans Android ;
    \item les règles d'accès (email non vérifié, rôle utilisateur) sont appliquées uniformément.
\end{itemize}

\section{Évolution future}

À mesure que le projet évolue et que de nouvelles fonctionnalités (progression, examens, certificats) sont ajoutées au backend, elles seront automatiquement disponibles pour les deux clients sans travail additionnel de client-side. Cette architecture facilite l'ajout de nouveaux clients (mobile iOS, applications natives desktop, etc.) à l'avenir.
```

## Summary

These sections cover:
1. **Methodology** - mention web version in parallel
2. **Web Architecture** - detailed chapter on structure, components, stack
3. **Technologies** - web tech stack with explanations
4. **Unified Backend** - how Android and web share same backend

Insert in order in the LaTeX file for logical flow.
