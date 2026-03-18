# API 개발 가이드

이 문서는 멀티모듈 구조에서 API를 개발하는 흐름을 설명합니다.
`example` 패키지의 코드를 참고하고, 실제 작업 시에는 `example`을 제외한 본인의 도메인 패키지를 생성하세요.

---

## 전체 흐름

```
HTTP 요청
   ↓
Controller          (api-server) — 요청 받기, BaseResponse로 감싸서 반환
   ↓
Service             (api-server) — 비즈니스 로직
   ↓
Repository          (domain)     — DB 접근
   ↓
Entity              (domain)     — 테이블 매핑
```

예외 발생 시:
```
Service/Controller
   ↓ throw BaseException(ErrorCode)
GlobalExceptionHandler  — 예외를 잡아 BaseResponse로 감싸서 반환
```

---

## 모듈별 역할

| 모듈     | 위치                              | 작성하는 것                                    |
|----------|-----------------------------------|------------------------------------------------|
| `core`   | `com.whyitrose.core`              | BaseResponse, BaseResponseStatus, BaseException |
| `domain` | `com.whyitrose.domain.{도메인}`   | Entity, Repository                             |
| `api-server` | `com.whyitrose.apiserver.{도메인}` | Controller, Service, DTO, 도메인 ErrorCode  |

---

## 예시 코드 구조

```
core/
├── response/
│   ├── ResponseStatus.java        ← 에러 코드 인터페이스
│   ├── BaseResponseStatus.java    ← 공통 에러 코드 (JWT, 인증 등)
│   └── BaseResponse.java          ← 공통 API 응답 래퍼
└── exception/
    └── BaseException.java         ← 공통 예외 클래스

domain/
└── example/post/
    ├── Post.java                  ← Entity
    └── PostRepository.java        ← Repository

api-server/
├── global/exception/
│   └── GlobalExceptionHandler.java  ← 전역 예외 처리
└── example/post/
    ├── controller/PostController.java
    ├── service/PostService.java
    ├── dto/
    │   ├── PostCreateRequest.java
    │   └── PostResponse.java
    └── exception/
        └── PostErrorCode.java     ← 도메인별 에러 코드
```

---

## Step 1. Entity 작성 (`domain` 모듈)

```java
// com.whyitrose.domain.{도메인}.Post
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA는 기본 생성자 필요, 외부에서 new 금지
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // auto increment
    private Long id;

    private String title;
    private String content;

    // new 대신 정적 팩토리 메서드로 생성
    public static Post create(String title, String content) {
        Post post = new Post();
        post.title = title;
        post.content = content;
        return post;
    }
}
```

**포인트**
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` — JPA가 내부적으로 기본 생성자를 사용하므로 필요하지만, 외부에서 직접 `new`를 막기 위해 `PROTECTED`로 설정합니다.
- setter 대신 정적 팩토리 메서드(`create`)로 객체를 생성합니다.

---

## Step 2. Repository 작성 (`domain` 모듈)

```java
// com.whyitrose.domain.{도메인}.PostRepository
public interface PostRepository extends JpaRepository<Post, Long> {
}
```

**포인트**
- `JpaRepository<엔티티, PK타입>`을 상속하면 `save`, `findById`, `findAll`, `delete` 등 기본 CRUD가 자동 제공됩니다.
- 추가 쿼리가 필요하면 메서드 이름으로 정의합니다.
  ```java
  List<Post> findByTitle(String title);
  ```

---

## Step 3. DTO 작성 (`api-server` 모듈)

**Request DTO** — 클라이언트에서 받는 값

```java
// com.whyitrose.apiserver.{도메인}.dto.PostCreateRequest
public record PostCreateRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}
```

**Response DTO** — 클라이언트에 반환하는 값

```java
// com.whyitrose.apiserver.{도메인}.dto.PostResponse
public record PostResponse(
        Long id,
        String title,
        String content
) {
    // Entity → DTO 변환 메서드
    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getTitle(), post.getContent());
    }
}
```

**포인트**
- Entity를 직접 반환하지 않고 DTO로 변환합니다. Entity가 변경되어도 API 응답 형태를 독립적으로 유지할 수 있습니다.
- `record`를 사용하면 생성자, getter, equals, toString이 자동 생성됩니다.
- `@NotBlank` 등 검증 어노테이션은 `spring-boot-starter-validation`에서 제공합니다.

---

## Step 4. 도메인 에러 코드 작성 (`api-server` 모듈)

도메인별 에러 코드는 `api-server` 모듈 내 해당 도메인 패키지 안에 정의합니다.
`ResponseStatus` 인터페이스를 구현하면 `BaseResponse`, `BaseException`과 함께 사용할 수 있습니다.

```java
// com.whyitrose.apiserver.{도메인}.exception.PostErrorCode
@Getter
@AllArgsConstructor
public enum PostErrorCode implements ResponseStatus {

    POST_NOT_FOUND(false, HttpStatus.NOT_FOUND, 3001, "존재하지 않는 게시글입니다."),
    UNAUTHORIZED_POST_ACCESS(false, HttpStatus.FORBIDDEN, 3002, "해당 게시글에 대한 권한이 없습니다.");

    private final boolean isSuccess;

    @JsonIgnore
    private final HttpStatus httpStatus;

    private final int responseCode;
    private final String responseMessage;
}
```

**포인트**
- 응답 코드는 도메인별로 범위를 나눠 관리합니다. (예: 3000번대 — Post, 4000번대 — User 등)
- `@JsonIgnore` — `httpStatus`는 HTTP 응답 헤더로만 사용하고 body에는 노출하지 않습니다.
- 공통 에러(JWT, 인증 등)는 `core`의 `BaseResponseStatus`에만 정의합니다.

---

## Step 5. Service 작성 (`api-server` 모듈)

```java
// com.whyitrose.apiserver.{도메인}.service.PostService
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본적으로 읽기 전용 트랜잭션
public class PostService {

    private final PostRepository postRepository;

    @Transactional  // 쓰기 작업에만 별도로 선언
    public PostResponse create(PostCreateRequest request) {
        Post post = Post.create(request.title(), request.content());
        return PostResponse.from(postRepository.save(post));
    }

    public PostResponse findById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BaseException(PostErrorCode.POST_NOT_FOUND));
        return PostResponse.from(post);
    }
}
```

**포인트**
- 클래스에 `@Transactional(readOnly = true)`를 선언하고, 쓰기 메서드에만 `@Transactional`을 추가합니다. 읽기 성능 최적화에 유리합니다.
- 데이터가 없는 경우 `BaseException`에 도메인 에러 코드를 담아 던집니다. `GlobalExceptionHandler`가 자동으로 처리합니다.

---

## Step 6. Controller 작성 (`api-server` 모듈)

```java
// com.whyitrose.apiserver.{도메인}.controller.PostController
@Tag(name = "Post", description = "게시글 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 생성")
    @PostMapping
    public ResponseEntity<BaseResponse<PostResponse>> create(@RequestBody @Valid PostCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(postService.create(request)));
    }

    @Operation(summary = "게시글 목록 조회")
    @GetMapping
    public ResponseEntity<BaseResponse<List<PostResponse>>> findAll() {
        return ResponseEntity.ok(BaseResponse.success(postService.findAll()));
    }
}
```

**포인트**
- `@Valid` — Request DTO에 선언한 `@NotBlank` 등을 실제로 검증합니다. 없으면 검증이 동작하지 않습니다.
- 응답은 항상 `ResponseEntity<BaseResponse<T>>`로 반환합니다. HTTP 상태 코드와 응답 body가 함께 관리됩니다.
- `BaseResponse.success(data)` — 성공 응답 생성.
- 에러는 `GlobalExceptionHandler`가 처리하므로 Controller에서 별도로 처리하지 않습니다.

---

## 공통 응답 형식

모든 API는 `BaseResponse<T>`로 감싸서 반환합니다.

**성공 응답**
```json
{
  "isSuccess": true,
  "responseCode": 1000,
  "responseMessage": "요청에 성공하였습니다.",
  "result": { "id": 1, "title": "제목", "content": "내용" }
}
```

**실패 응답**
```json
{
  "isSuccess": false,
  "responseCode": 3001,
  "responseMessage": "존재하지 않는 게시글입니다."
}
```

---

## Swagger

의존성을 추가하면 별도 설정 없이 자동으로 Swagger UI가 활성화됩니다.

```groovy
// api-server/build.gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6'
```

애플리케이션 실행 후 브라우저에서 확인할 수 있습니다.
```
http://localhost:8080/swagger-ui/index.html
```

| 어노테이션   | 위치   | 역할                                                   |
|--------------|--------|--------------------------------------------------------|
| `@Tag`       | 클래스 | Swagger UI에서 API 그룹 이름과 설명 표시               |
| `@Operation` | 메서드 | 개별 API의 요약(summary)과 상세 설명(description) 표시 |

---

## 실제 도메인 작업 시 체크리스트

- [ ] `domain` 모듈에 Entity, Repository 작성
- [ ] `api-server` 모듈에 도메인 ErrorCode 작성
- [ ] `api-server` 모듈에 DTO, Service, Controller 작성
- [ ] 패키지명에서 `example` 제거
- [ ] URL에서 `/example` prefix 제거

---

## 예시 API 테스트

애플리케이션 실행 후 아래로 테스트할 수 있습니다.

**게시글 생성**
```bash
curl -X POST http://localhost:8080/example/posts \
  -H "Content-Type: application/json" \
  -d '{"title": "제목", "content": "내용"}'
```

**게시글 목록 조회**
```bash
curl http://localhost:8080/example/posts
```