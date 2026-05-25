ALTER TABLE lessons ADD COLUMN content_url  VARCHAR(2048);
ALTER TABLE lessons ADD COLUMN content_body TEXT;

-- Seed content for preview lessons so the lesson player has something to show
UPDATE lessons SET content_url = 'https://www.youtube.com/embed/NybHckSEQBI'
 WHERE id = '11111111-aaaa-0000-0000-111111111111';

UPDATE lessons SET content_url = 'https://www.youtube.com/embed/ZM8ECpBuQYE'
 WHERE id = '22222222-aaaa-0000-0000-111111111111';

UPDATE lessons SET content_url = 'https://www.youtube.com/embed/Ge7c7otG2mk'
 WHERE id = '33333333-aaaa-0000-0000-111111111111';

UPDATE lessons SET content_url = 'https://www.youtube.com/embed/0fKg7e37bQE'
 WHERE id = '44444444-aaaa-0000-0000-111111111111';

UPDATE lessons SET content_url = 'https://www.youtube.com/embed/9bZkp7q19f0'
 WHERE id = '55555555-aaaa-0000-0000-111111111111';

-- Non-preview VIDEO lessons
UPDATE lessons SET content_url = 'https://www.youtube.com/embed/NybHckSEQBI'
 WHERE id = '11111111-aaaa-0000-0000-222222222222';

UPDATE lessons SET content_url = 'https://www.youtube.com/embed/NybHckSEQBI'
 WHERE id = '11111111-bbbb-0000-0000-111111111111';

-- ARTICLE lessons get a content_body instead
UPDATE lessons SET content_body = '## Reading Basic Functions

A **function** maps each input to exactly one output.

### Notation
- `f(x) = 2x + 1` means: multiply the input by 2, then add 1.
- `f(3) = 7`

### Tables
| x | f(x) |
|---|------|
| 0 | 1    |
| 1 | 3    |
| 2 | 5    |

### Key idea
If you see the same x giving two different y values, it is **not** a function.'
 WHERE id = '11111111-bbbb-0000-0000-222222222222';

UPDATE lessons SET content_body = '## Classroom Questions in English

Use these phrases to interact naturally in class.

### Asking for clarification
- *"Could you repeat that, please?"*
- *"What do you mean by ...?"*
- *"Is that the same as ...?"*

### Asking for examples
- *"Can you give an example?"*
- *"Could you show us how to do it?"*

### Admitting you do not understand
- *"I am not sure I follow."*
- *"I am lost — can we go back to the part about ...?"*

Practice these phrases in pairs before the next lesson.'
 WHERE id = '33333333-bbbb-0000-0000-111111111111';
