@echo off
set SPRING_DATASOURCE_PASSWORD=Savya9899
set GOOGLE_API_KEY=AIzaSyDHdJetbI-LOH-eGsU4gvrg5fyik6KSSEg
set JWT_SECRET=71323aacce8bfdb5d31875074badf176
set SPRING_MAIL_PASSWORD=re_Aq8tcD4T_BtJL3DJZ2aZF5W3m5yVJ6jZ1

echo Starting Mood Journal with local secrets...
cd moodjournal\backend
mvnw spring-boot:run
