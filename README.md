# 📊 AlgoTrade Bot – AngelOne Integration  

This project is a **Spring Boot + Thymeleaf** based trading dashboard integrated with the **AngelOne SmartAPI**.  
It provides a simple **web interface** for login, portfolio view, and stock search with live quotes.  

---

## ⚙️ Features  
- 🔑 **Login via AngelOne SmartAPI** (userId + TOTP)  
- 👤 **Profile fetch & display** (client code, email, mobile, exchanges, products)  
- 📊 **Portfolio dashboard** (read-only, equity view)  
- 🔍 **Stock search** (search by name/symbol, fetch live LTP & OHLC data)  
- 📈 **Live market data** (single/multiple instruments)  

---

## 🏗️ Architecture (Mermaid Diagram)
flowchart TD
    A[Browser - User UI] --> B[Spring Boot Controllers]
    B --> C[AngelOneService]
    B --> D[Thymeleaf Templates]

    C -->|Login, Portfolio, Profile, Market Data| E[AngelOne SmartAPI]

    D -->|Render Views| A
+----------------------------------------------------------+
| AlgoTrade Bot                                            |
+----------------------+-----------------------------------+
|      Sidebar         |          Main Content             |
|----------------------|-----------------------------------|
|  [🏠] Dashboard      |  Welcome, <UserName>              |
|  [📊] Portfolio      |                                   |
|  [🔍] Search Stock   |  ┌──────────────────────────┐     |
|  [👤] Profile        |  │  Portfolio Summary        │     |
|  [⚙️] Settings       |  │  Holdings, P&L, etc.     │     |
|                      |  └──────────────────────────┘     |
|                      |                                   |
|                      |  ┌──────────────────────────┐     |
|                      |  │  Stock Search            │     |
|                      |  │  [ Search Box   ][🔍]    │     |
|                      |  │  Results: LTP, OHLC, ... │     |
|                      |  └──────────────────────────┘     |
+----------------------+-----------------------------------+
---
## Example API Responses
-  🔑 Login / Profile Response
{
  "status": true,
  "message": "SUCCESS",
  "data": {
    "clientcode": "YOUR_CLIENT_CODE",
    "name": "YOUR_NAME",
    "email": "YOUR_EMAIL",
    "mobileno": "YOUR_PHONE_NUMBER",
    "exchanges": ["NSE", "BSE", "MCX", "CDS", "NCDEX", "NFO"],
    "products": ["DELIVERY", "INTRADAY", "MARGIN"],
    "brokerid": "B2C"
  }
}
-  📊 Holdings Response
{
  "status": true,
  "message": "SUCCESS",
  "data": {
    "holdings": [
      {
        "tradingsymbol": "TATASTEEL-EQ",
        "exchange": "NSE",
        "quantity": 2,
        "averageprice": 111.87,
        "ltp": 130.15,
        "profitandloss": 37,
        "pnlpercentage": 16.34
      },
      {
        "tradingsymbol": "SBIN-EQ",
        "exchange": "NSE",
        "quantity": 8,
        "averageprice": 573.1,
        "ltp": 579.05,
        "profitandloss": 48,
        "pnlpercentage": 1.04
      }
    ],
    "totalholding": {
      "totalholdingvalue": 5294,
      "totalinvvalue": 5116,
      "totalprofitandloss": 178.14,
      "totalpnlpercentage": 3.48
    }
  }
}

-  🔍 Search Scrip Response
{
  "status": true,
  "message": "SUCCESS",
  "data": [
    {
      "exchange": "NSE",
      "tradingsymbol": "SBIN-EQ",
      "symboltoken": "3045"
    },
    {
      "exchange": "NSE",
      "tradingsymbol": "SBIN-BE",
      "symboltoken": "4884"
    }
  ]
}

-  📈 Market Quote Response (FULL mode)
{
  "status": true,
  "message": "SUCCESS",
  "data": {
    "fetched": [
      {
        "exchange": "NSE",
        "tradingSymbol": "SBIN-EQ",
        "symbolToken": "3045",
        "ltp": 568.2,
        "open": 567.4,
        "high": 569.35,
        "low": 566.1,
        "close": 567.4,
        "tradeVolume": 3556150,
        "52WeekHigh": 629.55,
        "52WeekLow": 430.7
      }
    ],
    "unfetched": []
  }
}

---

## 🚀 Setup
1. Clone the repo
   
- git clone https://github.com/your-repo/algotrade-bot.git

-  cd algotrade-bot

## 2. Configure

Update application.properties with your AngelOne API credentials:
- angelone.api.key=YOUR_API_KEY
- angelone.client.id=YOUR_CLIENT_ID
- angelone.secret=YOUR_SECRET

## 3. Run the app
- mvn spring-boot:run
  
## 4. Access the UI
Open in browser:
- http://localhost:8080/

## 🔮 Roadmap

- Add order placement (Buy/Sell)

- Add Websocket streaming for live tick updates

- Add alerts/notifications
