import api from './axios'

export function getPrice(instrumentId){
    return api.get('/price', {params: {instrumentId: instrumentId}} )
}