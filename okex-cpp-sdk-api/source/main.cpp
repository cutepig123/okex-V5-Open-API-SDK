#include <iostream>
#include <cpprest/http_client.h>
#include "okapi.h"
#include "okapi_ws.h"
#include <algorithm>
#include "utils.h"
#include <windows.h>

string instrument_id = "BCH-USD-181228";
string order_id = "1641326222656512";
string currency  = "bch";

int main(int argc, char *args[]) {
//    OKAPI okapi;
    /************************** set config **********************/
    struct Config config;
    config.SecretKey = "";
    config.ApiKey = "";
    config.Endpoint = "https://www.okex.com";
    config.I18n = "en_US";
    config.IsPrint = true;
    config.Passphrase = "";

    //okapi.SetConfig(config);
    ///************************** test examples **********************/
    //if (0) {
    //    okapi.GetServerTime();
    //    okapi.GetCurrencies();
    //    //okapi.GetWalletCurrency(currency);
    //    okapi.GetWithdrawFee();
    //}
    value obj  = value::object(true);
    obj[L"instrument_id"] = value::string(s2w(instrument_id));
    obj[L"direction"] = value::string(L"long");

    value obj2;
    obj2[L"afds"] = value::string(L"sa");

    value abc = value::array();
    abc[0] = obj;
    abc[1] = obj2;
    wcout << abc.serialize() << std::endl;

    value obj3;
    obj3[L"asf"] = abc;
    wcout << obj3.serialize() << std::endl;

    value obj4 = value::object(true);
    obj4[L"afdf"] = obj;
    wcout << obj4.serialize() << std::endl;

    /************************** futures test examples **********************/
 /*   if (0) {
        value obj;
        okapi.GetFuturesPositions();
        okapi.GetFuturesInstrumentPosition(instrument_id);
        okapi.GetFuturesAccountsByCurrency(currency);
        okapi.GetFuturesLeverageByCurrency(currency);
        obj[L"instrument_id"] = value::string(instrument_id);
        obj[L"direction"] = value::string(L"long");
        obj[L"leverage"] = value::string(L"20");
        okapi.SetFuturesLeverageByCurrency(currency, obj);
        okapi.GetFuturesAccountsLedgerByCurrency(currency);

        obj[L"instrument_id"] = value::string(instrument_id);
        obj[L"type"] = value::number(2);
        obj[L"price"] = value::number(10000.1);
        obj[L"size"] = value::number(1);
        obj[L"margin_price"] = value::number(0);
        obj[L"leverage"] = value::number(10);
        okapi.FuturesOrder(obj);
        okapi.CancelFuturesInstrumentOrder(instrument_id, order_id);

        okapi.GetFuturesOrderList("2", instrument_id);
        okapi.GetFuturesOrder(instrument_id, order_id);
        okapi.GetFuturesFills(instrument_id, order_id);
        okapi.GetFuturesInstruments();
        okapi.GetFuturesInstrumentBook(instrument_id, 50);
        okapi.GetFuturesTicker();
        okapi.GetFuturesInstrumentTicker(instrument_id);
        okapi.GetFuturesInstrumentTrades(instrument_id);
        okapi.GetFuturesInstrumentCandles(instrument_id);
        okapi.GetFuturesIndex(instrument_id);
        okapi.GetFuturesRate();
        okapi.GetFuturesInstrumentEstimatedPrice(instrument_id);
        okapi.GetFuturesInstrumentOpenInterest(instrument_id);
        okapi.GetFuturesInstrumentPriceLimit(instrument_id);
        okapi.GetFuturesInstrumentLiquidation(instrument_id, 0);
        okapi.GetFuturesInstrumentHolds(instrument_id);
    }*/

    //if(1){
    //    string swap_instrument_id = "BTC-USD-SWAP";
    //    value postSwapCancelBatchOrderParams;
    //    value corders = value::array();
    //    value order1 = value::string(L"64-98-49f5f30f7-0");
    //    value order2 = value::string(L"64-98-49f5f30f8-0");
    //    corders[0] = order1;
    //    corders[1] = order2;
    //    postSwapCancelBatchOrderParams["ids"] = corders;
    //    //okapi.GetSwapMarketPrice(swap_instrument_id);
    //    okapi.CancelSwapInstrumentOrders(swap_instrument_id,postSwapCancelBatchOrderParams); // 批量撤单 形参应该是个json Object


    //}

    /************************** websocket test examples **********************/
    string uri = ("wss://real.okex.com:8443/ws/v3");
    //string uri = ("ws://192.168.80.113:10442/ws/v3?_compress=false");
    if (1) {
        //pplx::create_task([=] {
            okapi_ws::SubscribeWithoutLogin(uri, ("swap/ticker:BTC-USD-SWAP"));
        //});
        //Sleep(2000);
        //okapi_ws::UnsubscribeWithoutLogin(uri, U("swap/ticker:BTC-USD-SWAP"));

        //Sleep(2000);
       // pplx::create_task([=] {
            okapi_ws::Subscribe(uri, ("swap/account:BTC-USD-SWAP"), config.ApiKey, config.Passphrase, config.SecretKey);
       // });
        //Sleep(2000);
        //okapi_ws::Unsubscribe(uri, U("swap/account:BTC-USD-SWAP"), config.ApiKey, config.Passphrase, config.SecretKey);
    }

    //if (0) {
    //    //深度频道
    //    pplx::create_task([=] {
    //        okapi_ws::SubscribeWithoutLogin(uri, U("swap/depth:BTC-USD-SWAP"));
    //    });
    //    Sleep(20);
    //    okapi_ws::UnsubscribeWithoutLogin(uri, U("swap/depth:BTC-USD-SWAP"));
    //}

    //if (0) {
    //    //用户持仓频道
    //    pplx::create_task([=] {
    //                          okapi_ws::Subscribe(uri, U("swap/position:BTC-USD-SWAP"), config.ApiKey, config.Passphrase,
    //                                              config.SecretKey);
    //                      }
    //    );
    //    Sleep(20);
    //    okapi_ws::Unsubscribe(uri, U("swap/position:BTC-USD-SWAP"), config.ApiKey, config.Passphrase, config.SecretKey);
    //}

    //if (0) {
    //    //用户账户频道
    //    pplx::create_task([=] {
    //                          okapi_ws::Subscribe(uri, U("swap/account:BTC-USD-SWAP"), config.ApiKey, config.Passphrase,
    //                                              config.SecretKey);
    //                      }
    //    );
    //    Sleep(20);
    //    okapi_ws::Unsubscribe(uri, U("swap/account:BTC-USD-SWAP"), config.ApiKey, config.Passphrase, config.SecretKey);
    //}
    return 0;
}