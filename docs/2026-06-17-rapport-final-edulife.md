# Rapport Final - EduLife

## Page de garde

**Projet :** EduLife  
**Réalisé par :** BAGHDAD Mohamed et BAAKKA Monssef  
**Type :** Plateforme éducative marocaine web/mobile  
**Date du rapport :** 17 juin 2026

---

## Remerciements

Nous remercions les personnes qui ont accompagné ce projet par leurs retours, leurs remarques techniques et leur exigence sur la cohérence de l'ensemble. Ce travail a demandé autant de rigueur sur l'architecture que sur l'expérience utilisateur, et cette double exigence a largement contribué à la qualité actuelle d'EduLife.

## Résumé

EduLife est une plateforme d'apprentissage structurée pensée pour un public marocain. Le projet répond à un problème simple mais concret : beaucoup d'apprenants consomment des contenus utiles, mais sans parcours clair, sans suivi fiable, sans évaluation sérieuse et sans preuve de réussite réellement vérifiable. EduLife propose donc une chaîne cohérente : découverte d'un cours, inscription, étude des leçons, passage d'un examen final, puis génération d'un certificat.

L'inspection menée dans ce dépôt montre un projet déjà avancé sur trois surfaces complémentaires : un backend Spring Boot organisé en monolithe modulaire, une application Android Java/XML structurée en MVVM pragmatique et une application web React/TanStack reliée au backend réel. Les captures intégrées dans ce rapport ont été prises en direct depuis `http://localhost:8080` le 17 juin 2026.

## Introduction générale

Au Maroc, l'apprentissage numérique souffre souvent d'un éclatement des ressources. L'étudiant passe d'une vidéo à un document PDF, d'un groupe de discussion à un lien externe, sans savoir si l'ordre suivi est pertinent ni si le niveau atteint peut être validé. Cette fragmentation produit deux effets : une progression difficile à mesurer et une forte dépendance à des ressources dispersées.

EduLife a été conçu pour répondre à cette limite par un cadre plus structuré. L'idée n'est pas de multiplier les fonctionnalités accessoires, mais de rendre le parcours d'apprentissage lisible, progressif et vérifiable. Le projet s'appuie donc sur une architecture assez simple pour rester maintenable, mais assez solide pour gérer plusieurs rôles, des examens corrigés côté serveur et des certificats téléchargeables.

## Problématique

Le projet traite principalement les difficultés suivantes :

- l'apprenant ne sait pas toujours quoi apprendre ni dans quel ordre ;
- les contenus utiles sont souvent dispersés entre plusieurs plateformes ;
- les parcours ne disposent pas toujours d'un vrai suivi de progression ;
- l'évaluation finale est fréquemment absente ou peu fiable ;
- la preuve de réussite n'est pas toujours formalisée par un certificat vérifiable ;
- les enseignants et administrateurs ont besoin d'un cadre clair pour publier, vérifier et suivre les contenus.

## Objectifs du projet

### Objectifs fonctionnels

- permettre l'authentification sécurisée via Firebase ;
- synchroniser l'identité avec un utilisateur interne côté backend ;
- proposer un catalogue de cours structuré par sections et leçons ;
- gérer l'inscription, la progression, l'examen final et les certificats ;
- offrir des espaces adaptés aux rôles `LEARNER`, `TEACHER`, `GROUP_ADMIN` et `ADMIN` ;
- exposer des tableaux de bord utiles sans mélanger les permissions.

### Objectifs techniques

- conserver une architecture simple et testable ;
- éviter un passage prématuré aux microservices ;
- centraliser la sécurité côté serveur ;
- partager la même logique métier entre mobile et web ;
- garder des contrats d'API clairs entre clients et backend ;
- préparer l'évolution future sans surcharger le MVP.

## Présentation générale d'EduLife

EduLife est organisé autour d'un parcours d'apprentissage volontairement linéaire :

```text
Découvrir un cours -> S'inscrire -> Étudier -> Passer l'examen -> Réussir -> Obtenir un certificat
```

Ce socle est complété par des fonctions d'accompagnement déjà visibles dans le dépôt : planner d'étude, analytics, gamification, demandes d'enseignant, modération de contenus et portail de groupe. Le projet ne cherche pas encore à couvrir les paiements, les appels vidéo, la messagerie temps réel ou un assistant IA à mémoire persistante. Ce choix rend le périmètre plus crédible pour un MVP.

## Acteurs du système

| Acteur | Responsabilités principales | Restrictions observées |
| --- | --- | --- |
| Étudiant | Consulter les cours, s'inscrire, suivre les leçons, passer l'examen, consulter ses certificats, utiliser le planner, l'analytics et la gamification | N'accède ni aux données d'autres apprenants ni aux écrans d'administration |
| Enseignant | Gérer ses cours, structurer sections et leçons, préparer l'examen final, suivre des analytics liés à ses propres cours | Pas d'accès aux cours d'autres enseignants ni aux métriques plateforme |
| Group Admin | Gérer des groupes, attacher des membres et des cours, consulter l'analytics de groupe, approuver des contenus dans le périmètre groupe | N'agit pas comme administrateur global |
| Admin plateforme | Consulter les métriques globales, suivre les demandes d'enseignant, surveiller la publication des cours et les indicateurs clés | L'application web observée ne montre pas encore un module dédié de gestion fine de tous les utilisateurs |

## Parcours utilisateurs

```mermaid
flowchart LR
    A[Visiteur] --> B[Inscription / Connexion Firebase]
    B --> C[Sync identité backend]
    C --> D[Catalogue de cours]
    D --> E[Inscription au cours]
    E --> F[Leçons et progression]
    F --> G[Examen final]
    G -->|Réussite| H[Certificat téléchargeable]
    G -->|Échec| I[Compteur d'échecs + cooldown]
```

```mermaid
flowchart TD
    T[Enseignant] --> T1[Teaching Studio]
    T1 --> T2[Création / édition du cours]
    T2 --> T3[Organisation sections et leçons]
    T3 --> T4[Construction de l'examen]
    T --> T5[Analytics enseignant]
    G[Group Admin] --> G1[Portail groupes]
    G1 --> G2[Détail groupe]
    G1 --> G3[Approbations]
    G --> G4[Analytics groupe]
    A[Admin] --> A1[Dashboard global]
    A --> A2[Teacher requests]
    A --> A3[Analytics plateforme]
```

## Cahier des charges fonctionnel

| Domaine | Fonctionnalités constatées |
| --- | --- |
| Authentification | Inscription, connexion, vérification d'email, synchronisation backend, rôles contrôlés |
| Cours | Catalogue, recherche, détails du cours, sections, ressources |
| Apprentissage | Leçons, suivi de progression, cours inscrits |
| Évaluation | Examen MCQ, statut d'examen, tentative, résultat, cooldown |
| Certificats | Liste, détail, téléchargement, vérification publique par hash |
| Enseignant | Teaching Studio, gestion du contenu, builder d'examen, analytics |
| Groupes | Liste des groupes, détail, analytics, approbation |
| Administration | Dashboard global, teacher requests, analytics |
| Accompagnement | Planner, analytics étudiant, gamification, advisor |

## Périmètre MVP et évolution

| État | Éléments |
| --- | --- |
| Implémenté et visible dans le code | Auth Firebase, sync backend, RBAC, catalogue, inscription, progression, examens MCQ, certificats, dashboard admin, portail enseignant, groupe, analytics, gamification, advisor, suppression de compte |
| Partiellement implémenté | Parité complète Android/web, écrans d'administration détaillés pour tous les objets, gestion web plus fine des utilisateurs, certains workflows CMS encore concentrés dans un nombre réduit d'écrans |
| Travaux futurs identifiés | Notifications, discussions, paiements, mentorat, analytics plus avancées, stockage cloud plus riche, amélioration de l'advisor, meilleure symétrie mobile/web |

## Architecture globale

EduLife repose sur une architecture trois tiers enrichie par Firebase :

```mermaid
flowchart LR
    W[Client Web React] --> API[Backend Spring Boot]
    M[Client Android Java/XML] --> API
    API --> DB[(PostgreSQL)]
    API --> FS[Stockage fichiers / uploads / PDF]
    W --> FB[Firebase Auth]
    M --> FB
    FB --> API
```

Le point important est le suivant : Firebase ne remplace pas le backend. Firebase fournit l'identité, mais les droits, l'utilisateur interne, la progression, les examens, les certificats et les contrôles métier restent côté Spring Boot.

## Architecture backend

### Stack technique

Le backend est une application Spring Boot `3.5.14` en `Java 21`, avec :

- Spring Web ;
- Spring Security ;
- Spring Data JPA ;
- Spring Validation ;
- PostgreSQL ;
- Flyway pour les migrations ;
- Firebase Admin SDK pour vérifier les ID tokens ;
- Bucket4j pour le rate limiting ;
- Thymeleaf + OpenHTMLToPDF + ZXing pour les certificats PDF vérifiables.

### Pourquoi un monolithe modulaire est pertinent ici

Le choix d'un monolithe modulaire est cohérent avec la maturité du produit. EduLife doit gérer plusieurs domaines, mais ces domaines restent fortement liés : inscription, cours, progression, examens et certificats partagent le même noyau métier. Découper trop tôt en microservices compliquerait les transactions, la sécurité et la maintenance quotidienne sans gain réel pour un MVP.

### Organisation des modules

L'inspection du code montre `19` packages de premier niveau sous `backend/src/main/java/com/edulife` :

`account`, `admin`, `advisor`, `analytics`, `auth`, `certificates`, `common`, `config`, `courses`, `enrollments`, `exams`, `gamification`, `groups`, `profiles`, `progress`, `roles`, `security`, `teacherrequests`, `users`.

Cette organisation colle bien à une logique par domaine. Elle évite de mélanger les contrôleurs, la sécurité et la logique métier dans une structure plate.

### Structure interne

Les modules suivent majoritairement une séparation `controller / service / repository / dto / entity`. Cette structure n'est pas théorique : elle apparaît clairement dans les domaines centraux et facilite la lecture. Les contrôleurs restent des points d'entrée HTTP. Les règles métier importantes, comme le scoring d'examen, la génération de certificats ou les contrôles de périmètre, sont déplacées vers les services.

### Authentification et sécurité

Le flux d'authentification observé est le suivant :

1. le client s'authentifie auprès de Firebase ;
2. le client envoie un token Bearer au backend ;
3. le backend valide ce token via Firebase Admin ;
4. `/api/v1/auth/sync` crée ou met à jour l'utilisateur interne ;
5. le backend résout ensuite le rôle interne et applique les contrôles RBAC.

Ce modèle a deux avantages :

- le client ne décide jamais seul de son rôle métier ;
- l'application garde un `userId` interne découplé du `firebase_uid`.

### Base de données et migrations

Le backend contient `24` migrations Flyway, de `V1__init.sql` à `V24__certificate_dynamic_snapshots.sql`. Les migrations couvrent les tables cœur du produit : utilisateurs, cours, inscriptions, progression, profils, examens, certificats, groupes, demandes d'enseignant, gamification et traces advisor.

Cette stratégie donne une traçabilité correcte de l'évolution du schéma et évite le bricolage manuel de la base.

### Examens et certificats

La partie examens est correctement pensée sur le plan sécurité :

- les bonnes réponses restent côté serveur ;
- le client reçoit les questions, pas la solution ;
- la soumission est corrigée au backend ;
- le certificat n'est produit qu'après réussite ;
- la vérification publique passe par un hash de certificat.

Une nuance importante doit toutefois être signalée : la gouvernance projet verrouille un seuil de réussite à `80%`, mais la capture live de l'examen "Digital Skills for Study Productivity" affiche un seuil `70%`. Cela révèle une incohérence entre les décisions de référence et certaines données/écrans observés en exécution.

### Maintenabilité

Le backend est solide pour un MVP parce qu'il combine :

- un découpage métier lisible ;
- une sécurité recentrée côté serveur ;
- des migrations versionnées ;
- des tests ciblés sur plusieurs domaines sensibles ;
- des règles métier cohérentes sur les examens, certificats et groupes.

## Architecture de l'application mobile

### Stack et structure

L'application Android suit une approche `Java + XML` avec un MVVM pragmatique. Le fichier `app/build.gradle.kts` confirme :

- `Firebase Auth` ;
- `ViewModel` et `LiveData` ;
- `Retrofit` ;
- `OkHttp` avec logging interceptor ;
- `Navigation Component` ;
- `EncryptedSharedPreferences` ;
- `RecyclerView`, `Glide`, `Material`.

### Organisation fonctionnelle

Le dossier `app/src/main/java/com/baghdad/edulife/features` contient `12` grands ensembles :

`admin`, `advisor`, `analytics`, `auth`, `certificates`, `courses`, `exams`, `gamification`, `groupadmin`, `onboarding`, `profile`, `teacher`.

Cette organisation est conforme à une logique feature-first. Le code partagé se trouve dans `core`, en particulier pour le réseau, la session et le stockage local.

### Gestion réseau et session

Le client Android utilise un `ApiClient` Retrofit, un `FirebaseAuthInterceptor` pour attacher le token et un `FirebaseTokenAuthenticator` pour le refresh sur `401`. Ce point est important, car il traduit concrètement la règle du projet : le token Firebase peut expirer, mais le client ne doit ni perdre sa session trop tôt ni multiplier les requêtes de refresh sans contrôle.

### Maintenabilité mobile

La structure mobile est saine pour un MVP parce qu'elle évite les couches excessives. Les fragments gèrent l'UI, les `ViewModel` exposent l'état, les repositories centralisent l'accès réseau et le backend reste la source de vérité pour les opérations métier sensibles.

## Architecture web

### Stack observée

L'application web `guided-journey-lab` repose sur :

- React `19` ;
- TypeScript ;
- TanStack Start / TanStack Router ;
- TanStack React Query ;
- Tailwind CSS `v4` ;
- composants UI inspirés de `shadcn` et `Radix` ;
- Firebase côté client pour l'identité ;
- un client API typé dans `src/lib/api/client.ts`.

### Structure des routes

L'inspection du dossier `src/routes` montre `36` routes. Elles se répartissent en cinq familles :

- pages publiques : `/`, `/login`, `/register`, `/forgot-password`, `/certificates/verify/$hash` ;
- expérience étudiant : `/dashboard`, `/explore`, `/courses/...`, `/learn/...`, `/planner`, `/advisor`, `/analytics`, `/level`, `/certificates`, `/profile` ;
- espace enseignant : `/teach`, `/teach/$courseId`, `/teach/$courseId/exam` ;
- espace groupe : `/groups`, `/groups/$groupId`, `/approvals` ;
- espace admin : `/admin/dashboard`, `/admin/teacher-requests`, `/admin/analytics`.

### Navigation partagée et contrôle des rôles

Le web n'utilise pas seulement des redirections visuelles. Le fichier `auth-context.tsx` montre :

- une synchronisation explicite avec `/api/v1/auth/sync` ;
- des guards `RequireAuth`, `RequireTeacher`, `RequireGroupManager`, `RequireCourseApprover`, `RequireAdmin` ;
- des redirections par rôle pour empêcher l'accès à un portail non autorisé.

### Intégration backend

Le client API centralise les appels vers :

- authentification synchronisée ;
- profil ;
- catalogue ;
- inscriptions ;
- progression ;
- examens ;
- certificats ;
- analytics ;
- CMS enseignant ;
- groupes ;
- administration.

Cette centralisation est un bon point. Elle réduit le couplage entre les composants d'interface et la logique réseau.

### Design system

Les captures live montrent une interface visuellement cohérente : navigation haute partagée, cartes blanches, accents sombres, hiérarchie typographique claire, tableaux de bord différenciés selon les rôles. Le design est plus travaillé que sur un simple prototype académique, sans tomber dans un habillage purement démonstratif.

## Modèle de données

Le modèle métier se concentre sur quelques entités fortes :

- `User`, `Role`, `Profile` ;
- `Course`, `Section`, `Lesson`, `CourseResource` ;
- `Enrollment`, `Progress` ;
- `Exam`, `ExamQuestion`, `ExamChoice`, `ExamAttempt`, `ExamAnswer` ;
- `Certificate` ;
- `Group`, `GroupMembership`, `TeacherRequest` ;
- objets analytics et gamification côté agrégation.

```mermaid
erDiagram
    USER ||--o{ ENROLLMENT : enrolls
    USER ||--|| PROFILE : owns
    USER ||--o{ COURSE : authors
    COURSE ||--o{ COURSE_SECTION : contains
    COURSE_SECTION ||--o{ LESSON : contains
    COURSE ||--o{ ENROLLMENT : receives
    ENROLLMENT ||--|| PROGRESS : tracks
    COURSE ||--|| EXAM : ends_with
    EXAM ||--o{ EXAM_QUESTION : contains
    EXAM_QUESTION ||--o{ EXAM_CHOICE : offers
    USER ||--o{ EXAM_ATTEMPT : submits
    EXAM_ATTEMPT ||--o{ EXAM_ANSWER : stores
    EXAM_ATTEMPT ||--o| CERTIFICATE : may_generate
    GROUP ||--o{ GROUP_MEMBERSHIP : contains
    GROUP ||--o{ COURSE : scopes
    USER ||--o{ TEACHER_REQUEST : submits
```

## Sécurité

La sécurité est l'un des points les plus convaincants du projet.

### Authentification

- Firebase sert d'autorité d'identité ;
- le backend vérifie les tokens ;
- le client ne transmet pas librement un rôle métier final.

### Contrôle d'accès

- les routes web sont gardées côté client ;
- les endpoints restent protégés côté backend ;
- les écrans enseignant, groupe et admin sont séparés.

### Ownership checks

Le code et les parcours observés montrent une logique d'ownership raisonnable : analytics enseignant sur ses propres cours, groupe admin sur ses propres groupes, admin sur les vues globales.

### Examens

- réponses correctes conservées côté serveur ;
- soumission sécurisée côté backend ;
- tentative et cooldown intégrés ;
- certificat déclenché après réussite.

### Certificats

- téléchargement backend ;
- vérification publique par hash ;
- génération PDF côté serveur ;
- présence d'un identifiant de certificat visible dans les captures live.

### Durcissement

Le backend contient des tests et composants dédiés à la sécurité, en particulier autour du filtre Firebase, du CORS par défaut et du rate limiting. La présence de `Bucket4j` n'est pas décorative : elle s'inscrit dans une logique de protection concrète des endpoints exposés.

## Fonctionnalités réalisées

### Authentification

L'authentification est réelle et opérationnelle. Les clients passent par Firebase, puis le backend synchronise l'identité pour obtenir un utilisateur interne. Cette étape est visible dans le code, dans les appels API et dans la possibilité d'ouvrir des portails différents selon le rôle.

### Catalogue de cours

Le web montre un catalogue exploratoire, des fiches de cours détaillées et une organisation par sections/leçons. Le backend expose les données, et les captures live confirment un rendu connecté à une vraie source.

### Apprentissage

L'étudiant peut visualiser son tableau de bord, reprendre un cours, ouvrir une leçon et suivre sa progression. La continuité entre dashboard, cours détaillé et lecture de leçon est l'une des parties les plus abouties du parcours.

### Examens

Le projet dispose d'un examen final MCQ avec statut, tentative et soumission côté backend. Une capture live montre un écran d'examen en état actif pour un apprenant réel.

### Certificats

La liste des certificats, le détail et la vérification publique sont implémentés. La présence d'un hash public et d'un téléchargement backend crédibilise cette partie du produit.

### Career Advisor

L'advisor est bien présent dans le produit, mais il doit être présenté avec mesure. On observe un module d'accompagnement et des traces dédiées dans la base. En revanche, rien n'indique un assistant conversationnel avancé avec mémoire persistante. Il s'agit plutôt d'un accompagnement guidé déjà utile, mais encore perfectible.

### Planner

Le planner fait partie des modules différenciants visibles sur le web. Il renforce l'idée qu'EduLife n'est pas seulement un catalogue de cours, mais un cadre de progression.

### Analytics

Les analytics existent pour plusieurs rôles :

- analytics étudiant ;
- analytics enseignant ;
- analytics groupe ;
- analytics admin.

Cette stratification est intéressante, car chaque rôle voit une lecture différente du système.

### Gamification

La route `level` et les composants dédiés montrent une vraie couche de gamification : score, progression, niveau et badges. C'est un ajout cohérent tant qu'il reste au service de l'engagement pédagogique.

### Teacher CMS

Le portail enseignant permet déjà de manipuler un cours, des sections, des leçons, un examen final et des métriques propres à l'auteur du contenu. Ce n'est pas un CMS complet au sens d'une usine à contenu, mais c'est un socle fonctionnel crédible.

### Admin dashboard

Le dashboard admin est opérationnel en live. Au moment de la capture, il affichait notamment `8` utilisateurs, `8` enregistrements de cours, `7` inscriptions actives et `4` certificats.

### Group admin dashboard

Le portail groupe existe et affiche des groupes, un détail de groupe, des approbations et des analytics. Cette couche est intéressante car elle évite de limiter le produit à une relation simple étudiant/enseignant.

## Captures d'écran de l'application

Toutes les captures ci-dessous proviennent d'une session live ouverte sur `localhost:8080` le 17 juin 2026. Elles ont été générées automatiquement par le script [`2026-06-17-live-screenshot-capture.mjs`](./2026-06-17-live-screenshot-capture.mjs) et inventoriées dans [`2026-06-17-live-screenshot-inventory.md`](./2026-06-17-live-screenshot-inventory.md).

### Interface publique

![Page d'accueil EduLife](./2026-06-17-live-project-inspection-assets/public/landing.png)

*Figure 1 - Page d'accueil publique capturée en direct sur `localhost:8080`.*

![Écran de connexion](./2026-06-17-live-project-inspection-assets/public/login.png)

*Figure 2 - Formulaire de connexion public.*

![Vérification publique d'un certificat](./2026-06-17-live-project-inspection-assets/public/certificate-verify.png)

*Figure 3 - Vérification publique d'un certificat via un hash réel.*

### Interface étudiant

![Dashboard étudiant](./2026-06-17-live-project-inspection-assets/student/dashboard.png)

*Figure 4 - Tableau de bord étudiant avec reprise de parcours et indicateurs personnels.*

![Détail de cours](./2026-06-17-live-project-inspection-assets/student/course-detail.png)

*Figure 5 - Vue détaillée d'un cours inscrit avec sections, progression et accès à l'examen.*

![Lecture de leçon](./2026-06-17-live-project-inspection-assets/student/lesson.png)

*Figure 6 - Écran de lecture d'une leçon.*

![Examen MCQ](./2026-06-17-live-project-inspection-assets/student_exam/exam.png)

*Figure 7 - Examen final MCQ capturé dans un état réellement jouable.*

![Certificats étudiant](./2026-06-17-live-project-inspection-assets/student/certificates.png)

*Figure 8 - Liste des certificats obtenus par l'apprenant.*

![Analytics étudiant](./2026-06-17-live-project-inspection-assets/student/analytics.png)

*Figure 9 - Tableau analytics étudiant.*

![Career Advisor](./2026-06-17-live-project-inspection-assets/student/advisor.png)

*Figure 10 - Module d'accompagnement Career Advisor.*

### Interface enseignant

![Portail enseignant](./2026-06-17-live-project-inspection-assets/teacher/dashboard.png)

*Figure 11 - Portail principal de l'enseignant.*

![Gestion de cours enseignant](./2026-06-17-live-project-inspection-assets/teacher/course-management.png)

*Figure 12 - Gestion d'un cours avec sections, leçons et examen final.*

![Analytics enseignant](./2026-06-17-live-project-inspection-assets/teacher/analytics.png)

*Figure 13 - Vue analytics filtrée sur les cours possédés par l'enseignant.*

### Interface Group Admin

![Portail groupes](./2026-06-17-live-project-inspection-assets/group_admin/dashboard.png)

*Figure 14 - Tableau de bord du group admin avec la liste des groupes disponibles.*

![Détail groupe](./2026-06-17-live-project-inspection-assets/group_admin/group-detail.png)

*Figure 15 - Détail d'un groupe et de son périmètre de cours.*

![Approbations groupe](./2026-06-17-live-project-inspection-assets/group_admin/approvals.png)

*Figure 16 - Écran d'approbation dans le périmètre groupe.*

### Interface Admin

![Dashboard admin](./2026-06-17-live-project-inspection-assets/admin/dashboard.png)

*Figure 17 - Console admin globale avec indicateurs live.*

![Teacher requests admin](./2026-06-17-live-project-inspection-assets/admin/teacher-requests.png)

*Figure 18 - Vue de suivi des demandes d'enseignant.*

![Analytics admin](./2026-06-17-live-project-inspection-assets/admin/analytics.png)

*Figure 19 - Analytics plateforme pour l'administrateur.*

## Tests et validation

### Commandes exécutées

| Commande | Résultat | Observation |
| --- | --- | --- |
| `node docs/2026-06-17-live-screenshot-capture.mjs` | OK | `31` captures live générées depuis `localhost:8080` |
| `cd backend && .\\mvnw.cmd test` | Échec partiel | `39` suites, `238` tests, `10` erreurs toutes dans `AuthSyncControllerTest` |
| `cd guided-journey-lab && npm run build` | OK | Build client et SSR terminé |
| `cd guided-journey-lab && npm run lint` | Échec | `6311` erreurs majoritairement Prettier/formatage et retours CRLF |
| `.\\gradlew.bat :app:assembleDebug` | OK | Assemblage Android debug réussi |

### Analyse des échecs

Le backend n'est pas globalement en rupture. L'échec provient d'une seule suite : `AuthSyncControllerTest`. Les dix erreurs relèvent d'un problème de nettoyage de base de test, avec violation de clé étrangère entre `users` et `courses` (`courses_created_by_user_id_fkey`) lors d'un `deleteAll`.

Le lint web échoue massivement, mais l'échantillon des erreurs montre surtout une dette de formatage et d'uniformisation de fins de ligne, pas une panne fonctionnelle du produit. Le build de production web, lui, passe.

## Points forts du projet

- l'architecture backend est lisible et réaliste ;
- la séparation des rôles est concrète et visible dans l'exécution ;
- le flux apprenant principal est cohérent de bout en bout ;
- la correction d'examen reste côté serveur ;
- la certification est mieux traitée que dans beaucoup de projets académiques ;
- le projet existe réellement sur web et mobile, pas seulement en maquettes ;
- les analytics et la gamification enrichissent le produit sans remplacer le cœur métier ;
- le choix du monolithe modulaire évite une complexité prématurée.

## Difficultés rencontrées

Le projet réunit plusieurs rôles et plusieurs surfaces clientes, ce qui complique naturellement les synchronisations. Les difficultés les plus crédibles sont les suivantes :

- garder un comportement cohérent entre Android, web et backend ;
- faire coexister learner, teacher, group admin et admin sans confusion de périmètre ;
- sécuriser les examens tout en gardant une UX lisible ;
- produire des certificats exploitables, téléchargeables et vérifiables ;
- éviter de surcharger le MVP avec des fonctions séduisantes mais secondaires.

## Perspectives

- consolider la cohérence des règles métier entre documentation, seed data et interfaces ;
- élargir les modules de modération admin ;
- approfondir l'advisor avec un meilleur niveau de personnalisation ;
- améliorer la parité Android/web ;
- intégrer à terme notifications, discussions, paiements et services de mentorat ;
- renforcer encore l'observabilité et la qualité de build/lint.

## Conclusion générale

EduLife apparaît comme un projet sérieux, mieux structuré que la moyenne des plateformes académiques construites trop vite autour d'un simple catalogue de pages. Le dépôt montre une vraie réflexion sur le cœur du produit : identité, rôles, progression, examen et certificat. Le backend tient une place centrale et raisonnable, le web offre déjà une expérience crédible, et l'application Android reste alignée avec les choix d'architecture annoncés.

Le projet n'est pas terminé, et il ne faut pas le présenter comme tel. Il reste de la dette technique, des modules encore partiels et quelques incohérences à corriger. Malgré cela, EduLife dispose déjà d'une base solide, cohérente et extensible pour devenir une plateforme éducative marocaine réellement exploitable.

## Annexes

### Annexe A - Inventaire backend

- `19` modules backend de premier niveau.
- `24` migrations Flyway.
- `39` suites de tests générées par Surefire.
- `238` tests backend exécutés lors de cette inspection.

### Annexe B - Inventaire principal des routes web

**Public :** `/`, `/login`, `/register`, `/forgot-password`, `/certificates/verify/$hash`  
**Étudiant :** `/dashboard`, `/explore`, `/courses`, `/courses/$courseId`, `/courses/$courseId/resources`, `/learn/$courseId/$lessonId`, `/courses/$courseId/exam`, `/courses/$courseId/exam/result`, `/planner`, `/advisor`, `/analytics`, `/level`, `/certificates`, `/certificates/$certificateId`, `/profile`  
**Enseignant :** `/teach`, `/teach/$courseId`, `/teach/$courseId/exam`  
**Group Admin :** `/groups`, `/groups/$groupId`, `/approvals`  
**Admin :** `/admin/dashboard`, `/admin/teacher-requests`, `/admin/analytics`

### Annexe C - Inventaire des captures live

Le détail complet des `31` captures figure dans [`2026-06-17-live-screenshot-inventory.md`](./2026-06-17-live-screenshot-inventory.md).

### Annexe D - Captures non réalisées comme écrans dédiés

| Écran demandé | Statut | Motif |
| --- | --- | --- |
| Création de cours enseignant dédiée | Non capturée comme page distincte | La web app observée expose surtout une page d'édition/gestion via `/teach/$courseId` |
| Monitoring étudiant enseignant dédié | Non capturé comme page distincte | La supervision passe surtout par les analytics, sans route roster dédiée constatée |
| Gestion utilisateurs admin dédiée | Non capturée | Aucune route web dédiée repérée pendant l'inspection |
| Modération certificats admin dédiée | Non capturée | Aucune route web dédiée repérée pendant l'inspection |
| Gestion fine enseignants/cours groupe en pages séparées | Non capturée comme écrans distincts | Fonctionnalités agrégées dans le détail groupe et les approbations |

### Annexe E - Statut de validation

- Screenshots live : **OK**
- Build web production : **OK**
- Build Android debug : **OK**
- Lint web : **KO**
- Tests backend : **KO partiel**
