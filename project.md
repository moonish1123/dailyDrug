프로젝트명: 매일 약먹기 
앱의 개략적 설명: 매일 먹어야 하는 약을 알림으로 보여준다 먹을때 까지 알림을 보여준다. 
UX 스타일: 블루 계열의 시원하고 모던하고 심플한 앱 

앱의 시나리오) 
메인화면에 진입. 기존 투약 스케줄이 있으면 그걸 보여주고 하단에 투약 스케줄 입력 버튼을 둬서 누르면 입력을 할 수 있는 화면으로 진입한다. 
메인화면은 현재 먹어야 할 약만 보여준다. 그리고 먹었는지 안먹었는지 보여주고 안먹었으면 빨간색으로 터치를 유도하고 먹었으면 편안한 푸른 계열로 표시한다. 
약을 투약해야 하기 위해 정보를 입력하는 화면은 연속투약기간 + 쉬는 기간으로 구성할수 있게 한다. 
예를 들면 매일, 5일먹고 하루 쉬기, 4일먹고 이틀쉬기 이런식이 입력 될수 있도록 입력폼을 넣는다. 가령 퐁당퐁당은 1일먹고 1일쉬고가 반복이면 되겠다. 

약을 안먹었으면 먹을때 까지 1시간마다 한번씩 알림을 보낸다. Toast, HighLevel priorty (notification channel 을 hight 로 알림, 소리 나게) 
포그라운드 서비스는 필요 없겠지 ? 필요하다면 추가한다. 

화면은 MainActivity 하나 compose 로 multi-screen 을 navhost 로 관리한다 
Clean architecture 를 준수하고 usecase, repository, datasource 를 유지한다.
database 는 room 을 api 는 필요하면 okhttp, retrofit 을 사용한다. 

일단 local 에 정보를 입력하고, 
정확한 알림을 줘야 하기 때문에 정확한 알림을 위한 권한을 정의 하고 런타임에 요청한다. 

구현 순서를 

모델링 정의 
-> datasource -> repository -> usecase 정의 

viewModel 정의 
MainActivity, Screen 의 정의 등의 순서로 진행한다. 
