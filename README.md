## 작업하게 된 계기

새로운 프로젝트를 시작하며, 기존의 프로젝트에서 아쉬웠던 점을 팀원들과 나눴다.

<br>

나왔던 내용 중에, 기간에 쫒겨 제대로된 코드리뷰를 하지 못해서

다른 팀원들의 코드에 대한 이해가 낮다는 이야기가 있었다.

<br>

그래서 이번 프로젝트 기간에서는

정기적으로 데일리 스크럼과 코드 설명회를 가지기로 하였다.

<br>

하지만 이것 만으로는 충분하지 않을 것 같다는 생각을 했다.

결국 프로젝트 마감이 다가오면 시간이 없어서

자주 만나지 못할 것 같고 이전의 문제가 반복되지 않을까하는 걱정이 들었다.

<br>

그래서 고민하던 중,

이전 3차 프로젝트 발표 중에 깃허브 액션을 사용하여

[AI 코드 리뷰를 구현한 팀](https://github.com/prgrms-be-devcourse/NBE3-4-3-Team12/blob/main/.github/workflows/gemini-code-review.yml)이 있었다.

이것에서 아이디어를 얻어서

PR 을 올리면 AI 가 해당 PR 의 작업에서 이루어진 내용들을 요약하는 워크플로를 만들어보기로 했다.

<br>

과정은 간단했지만 원하는 출력 형식을 맞추기 위해

프롬프트를 많이 수정해야해서 시간은 좀 많이 들었다.

새로운 프로젝트를 시작하면서 적용해볼 예정인데

효과적이고 효율적인 코드리뷰 문화가 자리잡는데 도움이 되면 좋겠다.

---

# 구현

1. 스프링부트 샘플 프로젝트 생성

![](https://velog.velcdn.com/images/dlsdn1996/post/f2b01f59-fef5-403a-a678-570761be82c9/image.png)

<br>

2. PR 작성

![](https://velog.velcdn.com/images/dlsdn1996/post/552c0c9e-c731-41c6-8836-0a5f5081ddd2/image.png)

<br>

3. check 들이 실행됨 (CI 테스트도 함께 적용한 모습)

![](https://velog.velcdn.com/images/dlsdn1996/post/1e04fe84-ff92-471b-a7ab-a4471ee3bd6c/image.png)

<br>

4. AI 의 요약이 작성됨

![](https://velog.velcdn.com/images/dlsdn1996/post/79647b8a-9d18-4ed7-a50d-396f512948bc/image.png)


---

# 워크플로 내용

~~~yaml
name: Gemini PR Summary and Explanation

on:
  pull_request:
    types: [opened, synchronize]  # PR이 열리거나 업데이트될 때 트리거됨

jobs:
  pr-summary:
    runs-on: ubuntu-latest  # Ubuntu 최신 버전에서 실행

    permissions:
      contents: read  # 리포지토리의 콘텐츠를 읽을 수 있음
      pull-requests: write  # PR에 댓글을 작성할 수 있음

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v3  # 리포지토리 코드 체크아웃
        with:
          fetch-depth: 0  # 전체 히스토리를 가져옵니다 (전체 히스토리 필요)

      - name: Set up Node.js
        uses: actions/setup-node@v3  # Node.js 환경 설정

      - name: Install Gemini AI Dependencies
        run: npm install @google/generative-ai  # Gemini AI 종속성 설치

      # PR 이벤트의 변경사항(diff) 추출
      - name: Fetch and Generate Git Diff for PR
        run: |
          git fetch origin "${{ github.event.pull_request.base.ref }}"  # 기본 브랜치에서 변경사항 가져오기
          git fetch origin "${{ github.event.pull_request.head.ref }}"   # PR 브랜치에서 변경사항 가져오기
          git diff --unified=0 "origin/${{ github.event.pull_request.base.ref }}" > diff.txt  # 변경된 diff 파일 생성

      # Gemini API를 호출하여 PR 제목, 설명, diff를 포함한 프롬프트로 요약 및 설명 생성
      - name: Call Gemini API to Generate PR Summary
        id: gemini_review
        uses: actions/github-script@v7  # GitHub API를 사용하여 스크립트 실행
        with:
          script: |
            const fs = require("fs");
            const diff_output = fs.readFileSync("diff.txt", 'utf8');  // diff.txt 파일 읽기
            
            const { GoogleGenerativeAI } = require("@google/generative-ai");
            const genAI = new GoogleGenerativeAI("${{ secrets.GEMINI_API_KEY }}");  // Gemini API 키로 생성자 인스턴스 생성
            const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });  // Gemini 모델 설정

            let prompt = `
              다음은 깃허브 PR 에 올라온 수정된 코드들 입니다.
              Git diff를 분석하고, 각 변경 사항에 대해 파일명, 수정 내용, 역할을 아래와 같은 형식을 반드시 유지하여 요약해 주세요.

              가장 첫 부분에는 이 PR을 최종 요약하여 알려주세요. 최종 요약 부분의 제목은 "### PR 요약 :" 으로 해주세요.
              PR 요약의 모든 문장은 끝나면 <br>으로 띄어주세요. PR 요약이 끝난 후에는 <hr>을 하나 넣어주세요.

              (각 파일에 대한 요약은 반드시 코드 블럭으로 들어가지 않게 해주세요)
              
            	### <　경로를 제외한 파일명　> (이 부분의 <  >는 유지되게 해주세요)
            	- **역할 :**
            	(이 변경 사항의 역할 예: 기능 추가, 버그 수정, 리펙토링 등)
            	
            	- **수정 내용 :**
            	(수정된 내용의 간략한 설명. 모든 문장은 끝나면 <br>으로 띄어주세요. 이 부분에서는 줄을 바꾸고 들여쓰기가 유지되도록 띄어주세요.)

              각 파일 내용이 끝나면 <br>으로 한 줄을 띄워주세요.
              
              <git diff>${diff_output}</git diff>`;  // diff 내용과 함께 요약 프롬프트 작성

            try {
              const result = await model.generateContent(prompt);  // Gemini API 호출하여 요약 생성
              const response = await result.response;
              const text = await response.text();

              if (!text || text.trim().length === 0) {
                console.log("❌ Gemini API 응답이 비어 있습니다.");
                throw new Error("Gemini API 응답이 비어 있습니다.");
              }

              fs.writeFileSync('review_result.txt', text);  // 응답을 파일로 저장
              console.log("✅ Gemini API 응답을 review_result.txt 파일에 저장했습니다.");
            } catch (error) {
              console.error("❌ Gemini API 요청 중 오류 발생:", error);  // 오류 발생 시 에러 메시지 출력
              process.exit(1);  // 워크플로 종료
            }

      - name: Format PR Review Summary for Comment
        id: store
        run: |
          COMMENT_STRING=$(cat review_result.txt)  # 생성된 요약을 파일에서 읽어옴
          
          # 줄바꿈 처리
          echo "comment<<EOF" >> $GITHUB_OUTPUT
          echo "# AI PR 요약" >> $GITHUB_OUTPUT
          echo -e "$COMMENT_STRING" >> $GITHUB_OUTPUT  # PR 요약을 댓글 형식으로 준비
          echo "EOF" >> $GITHUB_OUTPUT

      - name: Post PR Summary Comment
        uses: mshick/add-pr-comment@v2  # PR에 요약을 댓글로 추가
        with:
          message: ${{ steps.store.outputs.comment }}  # 준비된 댓글 메시지 사용
          repo-token: ${{ secrets.GITHUB_TOKEN }}  # GitHub 토큰 사용
~~~

앞 부분의 세팅을 제외하면

1. 변경사항을 추출하고,
2. AI 를 호출하고,
3. 결과를 가공해서,
4. 댓글로 올리는

4단계로 이루어져 있다.

<br>

마지막 결과를 댓글로 다는 부분이 고민이었는데

[Add PR Comment](https://github.com/marketplace/actions/add-pr-comment) 라는 액션을 발견해서 쉽게 적용할 수 있었다.

<br>

찾아봤던 여러가지 액션들 중에서 이 액션을 택한 가장 큰 이유는

댓글이 "sticky" 방식으로 작성된다는 것이다.

그래서 새로운 커밋으로 PR이 업데이트되면

기존의 코멘트를 수정해서 새로운 내용으로 업데이트 한다.

<br>

다른 방식들은 코멘트 들이 계속 추가되었는데

10~20 개정도가 넘어가면 PR 페이지를 열 때

시간이 오래 걸리게 되는 것 같았다.

---

# 적용을 위해 필요한 내용

깃허브 리포지터리에 해당 내용을 적용하기 위해서는 2가지가 필요하다.

### 1. ".github/workflows/워크플로.yml" 작성

![](https://velog.velcdn.com/images/dlsdn1996/post/be18d823-8cb4-46ee-b9e9-6ad403a37105/image.png)

디렉토리 구조가 정확해야 한다.
( 루트 디렉토리에 .github 디렉토리를 생성해야 한다. )

이전에 디렉토리 이름을 "workflow" 로 실수한 적이 있는데 작동되지 않았다.

<br>

### 2. Gemini API Key

![](https://velog.velcdn.com/images/dlsdn1996/post/37a38ab0-1bf5-4f41-bb11-904e8597ef15/image.png)

구글 AI 스튜디오에서 API 키를 받아온다.

![](https://velog.velcdn.com/images/dlsdn1996/post/f72c223f-5abb-4216-80a4-1e7dbb1cc77c/image.png)

깃허브 시크릿에 GEMINI_API_KEY 라는 이름으로 등록한다.

이름은 yml 파일에서 사용한 이름과 동일해야 한다.

---

# 추가 고민사항

지금은 일단 PR 에서 코드 수정 내역만 가져와서 확인하고 있다.

PR 의 제목과 내용까지 가져와서 확인하면

더 정확한 요약이 가능할 것 같은데

그러면 AI 에게 보내지는 내용이 너무 많아 질 것 같아서

일단은 적용하지 않았다.

<br>

<br>

한달정도 프로젝트 후에

실제 프로젝트 중에도 AI 요약이 잘 작동했는지,

추가하거나 수정할 내용은 없는지,

팀원들의 반응은 어땠는지 추가하면 좋겠다.
