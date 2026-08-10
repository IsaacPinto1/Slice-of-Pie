import api from './axios'

export function getPrice(ticker){
    return api.get('/price', {params: {ticker : ticker}} )
}